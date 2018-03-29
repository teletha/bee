/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MXBean;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import bee.TaskFailure;
import bee.UserInterface;
import bee.api.Command;
import bee.api.Library;
import filer.Filer;
import kiss.Decoder;
import kiss.Encoder;
import kiss.I;

/**
 * @version 2016/12/12 14:40:20
 */
public class Java {

    /** The identifiable name. */
    private static final ObjectName NAME;

    static {
        try {
            NAME = new ObjectName(Transporter.class.getPackage().getName(), "type", Transporter.class.getSimpleName());
        } catch (MalformedObjectNameException e) {
            throw I.quiet(e);
        }
    }

    /** The classpaths. */
    private final List<Path> classpaths = new ArrayList();

    /** The assertion usage state. */
    private boolean enableAssertion = false;

    /** The working directory. */
    private Path directory;

    /** {@link System#out} and {@link System#in} encoding. */
    private Charset encoding;

    /** The execution type. */
    private boolean sync = true;

    /** Xms option. */
    private int initialMemory = 256;

    /** Xmx option. */
    private int maxMemory = 2048;

    /**
     * Hide Constructor.
     */
    private Java() {
    }

    /**
     * <p>
     * Create JVM instance.
     * </p>
     */
    public static Java with() {
        return new Java();
    }

    /**
     * <p>
     * Add classpath.
     * </p>
     * 
     * @param path
     */
    public Java classPath(Collection<Library> paths) {
        if (paths != null) {
            for (Library library : paths) {
                classpaths.add(library.getLocalJar());
            }
        }

        // API definition
        return this;
    }

    /**
     * <p>
     * Add classpath.
     * </p>
     * 
     * @param path
     */
    public Java classPath(Path path) {
        if (path != null) {
            classpaths.add(path.toAbsolutePath());
        }

        // API definition
        return this;
    }

    /**
     * <p>
     * Add classpath.
     * </p>
     * 
     * @param path
     */
    public Java classPath(Class... classes) {
        if (classes != null) {
            for (Class clazz : classes) {
                classPath(Filer.locate(clazz));
            }
        }

        // API definition
        return this;
    }

    /**
     * Configure memory setting.
     * 
     * @param initialMemory A initial memory size (MB).
     * @param maxMemory A max memory size (MB).
     * @return
     */
    public Java memory(int initialMemory, int maxMemory) {
        if (0 < initialMemory) this.initialMemory = initialMemory;
        if (0 < maxMemory) this.maxMemory = maxMemory;

        // API definition
        return this;
    }

    /**
     * <p>
     * Enable assertion functionality.
     * </p>
     */
    public Java enableAssertion() {
        enableAssertion = true;

        // API definition
        return this;
    }

    /**
     * <p>
     * Make this process running asynchronously.
     * </p>
     * 
     * @return Fluent API.
     */
    public Java inParallel() {
        this.sync = false;

        // API definition
        return this;
    }

    /**
     * <p>
     * Set working directory.
     * </p>
     * 
     * @param directory A location of working directory.
     */
    public Java workingDirectory(Path directory) {
        this.directory = directory;

        // API definition
        return this;
    }

    /**
     * <p>
     * Set {@link System#out} and {@link System#in} encoding.
     * </p>
     * 
     * @param encoding A {@link Charset} to set.
     * @return
     */
    public Java encoding(Charset encoding) {
        this.encoding = encoding;

        // API definition
        return this;
    }

    /**
     * <p>
     * Execute this java command.
     * </p>
     */
    public void run(Class<? extends JVM> mainClass, Object... arguments) {
        // create address
        int port = unboundedPort();
        String address = "service:jmx:rmi:///jndi/rmi://localhost:" + port + "/hello";

        List<String> command = new ArrayList();
        command.add("java");

        if (classpaths.size() != 0) {
            command.add("-cp");
            command.add(I.join(File.pathSeparator, classpaths));
        }

        if (enableAssertion) {
            command.add("-ea");
        }

        command.add("-Dfile.encoding=UTF-8");
        command.add("-Xms" + initialMemory + "m");
        command.add("-Xmx" + maxMemory + "m");

        command.add(JVM.class.getName());
        command.add(address);
        command.add(String.valueOf(sync));
        command.add(mainClass.getName());

        for (Object argument : arguments) {
            command.add(argument.toString());
        }

        if (!sync) {
            // build sub-process for java
            Process.with().workingDirectory(directory).encoding(encoding).inParallel().run(command);
        } else {
            JVMTransporter listener = I.make(JVMTransporter.class);

            try {
                LocateRegistry.createRegistry(port);
                MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                server.registerMBean(listener, NAME);

                JMXConnectorServer connector = JMXConnectorServerFactory.newJMXConnectorServer(new JMXServiceURL(address), null, server);
                connector.start();

                // build sub-process for java
                Process.with().workingDirectory(directory).encoding(encoding).run(command);

                connector.stop();
            } catch (Throwable e) {
                Path path = Paths.get("E:\\error.txt");
                try {
                    e.printStackTrace(new PrintWriter(path.toFile()));
                } catch (FileNotFoundException e1) {
                    throw I.quiet(e);
                }

                throw I.quiet(e);
            } finally {
                if (listener.error != null) {
                    throw I.quiet(listener.error);
                }
            }
        }
    }

    /**
     * Search unbounded port.
     * 
     * @return
     */
    private int unboundedPort() {
        try (Socket socket = new Socket()) {
            socket.bind(null);
            return socket.getLocalPort();
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * @version 2016/12/12 14:40:14
     */
    public static abstract class JVM {

        /** The user interface of the parent process. */
        protected final UserInterface ui = new JVMUserInterface();

        /** The activation argument. */
        protected String[] args;

        /**
         * <p>
         * Write sub-process code.
         * </p>
         * 
         * @throws Exception Execution error.
         */
        protected abstract void process() throws Exception;

        /**
         * <p>
         * Main method is activation point of sub process JVM.
         * </p>
         */
        public static void main(String[] args) throws Exception {
            // load stacktrace codec
            I.load(StackTraceCodec.class, true);

            // execute main process
            JVM vm = (JVM) I.make(Class.forName(args[2]));

            try {
                // check sync mode
                if (args[1].equals("true")) {
                    // launch observer
                    JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(args[0]));
                    MBeanServerConnection connection = connector.getMBeanServerConnection();

                    // create transporter proxy
                    Transporter transporter = MBeanServerInvocationHandler.newProxyInstance(connection, NAME, Transporter.class, false);

                    ((JVMUserInterface) vm.ui).transporter = transporter;
                }

                vm.args = Arrays.copyOfRange(args, 3, args.length);
                vm.process();
            } catch (Throwable e) {
                vm.ui.error(e);
            } finally {
                System.exit(0);
            }
        }

        /**
         * <p>
         * This class is {@link UserInterface} wrapper of {@link Transporter} for interprocess
         * communication.
         * </p>
         * <p>
         * Must be non-static class to hide from class scanning.
         * </p>
         * 
         * @version 2012/04/09 16:58:06
         */
        private final class JVMUserInterface extends UserInterface {

            /** The event transporter. */
            private Transporter transporter;

            /**
             * {@inheritDoc}
             */
            @Override
            public String ask(String question) {
                // If this exception will be thrown, it is bug of this program. So we must rethrow
                // the wrapped error in here.
                throw new Error();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public <T> T ask(String question, T defaultAnswer) {
                // If this exception will be thrown, it is bug of this program. So we must rethrow
                // the wrapped error in here.
                throw new Error();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public <T> T ask(String question, List<T> items) {
                // If this exception will be thrown, it is bug of this program. So we must rethrow
                // the wrapped error in here.
                throw new Error();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void title(CharSequence title) {
                transporter.title(title.toString());
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void talk(Object... messages) {
                transporter.talk(UserInterface.build(messages));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void warn(Object... messages) {
                transporter.talk(UserInterface.build(messages));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void error(Object... messages) {
                for (Object message : messages) {
                    if (message instanceof Throwable) {
                        StringBuilder builder = new StringBuilder();
                        Cause cause = make((Throwable) message);
                        I.write(cause, builder);

                        transporter.error(builder.toString());
                    }
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Appendable getInterface() {
                // If this exception will be thrown, it is bug of this program. So we must rethrow
                // the wrapped error in here.
                throw new Error();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void startCommand(String name, Command command) {
                // If this exception will be thrown, it is bug of this program. So we must rethrow
                // the wrapped error in here.
                throw new Error();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void endCommand(String name, Command command) {
                // If this exception will be thrown, it is bug of this program. So we must rethrow
                // the wrapped error in here.
                throw new Error();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void write(String message) {
                // If this exception will be thrown, it is bug of this program. So we must rethrow
                // the wrapped error in here.
                throw new Error();
            }

            /**
             * <p>
             * Create cause.
             * </p>
             * 
             * @param throwable
             * @return
             */
            private Cause make(Throwable throwable) {
                Cause cause = new Cause();
                cause.message = throwable.getMessage();

                if (throwable instanceof TaskFailure) {
                    TaskFailure failure = (TaskFailure) throwable;
                    cause.reason = failure.reason;
                    cause.solution.addAll(failure.solution);
                }

                for (StackTraceElement element : throwable.getStackTrace()) {
                    cause.traces.add(element);
                }

                // API definition
                return cause;
            }
        }
    }

    /**
     * <p>
     * Transporter between parent process and sub process.
     * </p>
     * 
     * @version 2012/04/04 23:03:00
     */
    @MXBean
    public static interface Transporter {

        /**
         * <p>
         * Talk to user.
         * </p>
         * 
         * @param message
         */
        void talk(String message);

        /**
         * <p>
         * Talk to user.
         * </p>
         * 
         * @param message
         */
        void title(String message);

        /**
         * <p>
         * Talk to user.
         * </p>
         * 
         * @param message
         */
        void warn(String message);

        /**
         * <p>
         * Error message
         * </p>
         * 
         * @param error
         */
        void error(String error);
    }

    /**
     * @version 2012/04/09 16:58:22
     */
    private static final class JVMTransporter extends StandardMBean implements Transporter {

        /** The actual user interface. */
        private final UserInterface ui;

        /** The sub process state. */
        private Throwable error;

        /**
         * Listen sub process event.
         */
        private JVMTransporter(UserInterface ui) throws Exception {
            super(Transporter.class);

            this.ui = ui;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void talk(String message) {
            ui.talk(message);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void title(String message) {
            ui.title(message);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void warn(String message) {
            ui.warn(message);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void error(String error) {
            this.error = I.read(error, new Cause());
        }
    }

    /**
     * @version 2014/07/28 14:16:07
     */
    @SuppressWarnings("serial")
    private static final class Cause extends TaskFailure {

        /** The error message. */
        @SuppressWarnings("unused")
        public String message;

        /** The stack trace. */
        public List<StackTraceElement> traces = new ArrayList();
    }

    /**
     * @version 2012/04/09 16:58:31
     */
    @SuppressWarnings("unused")
    private static final class StackTraceCodec implements Decoder<StackTraceElement>, Encoder<StackTraceElement> {

        /**
         * {@inheritDoc}
         */
        @Override
        public String encode(StackTraceElement value) {
            return value.getClassName() + " " + value.getMethodName() + " " + value.getFileName() + " " + value.getLineNumber();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public StackTraceElement decode(String value) {
            String[] values = value.split(" ");
            return new StackTraceElement(values[0], values[1], values[2], Integer.parseInt(values[3]));
        }
    }

    /**
     * @version 2016/10/12 10:53:26
     */
    @SuppressWarnings("unused")
    private static final class StackTracesCodec implements Decoder<StackTraceElement[]>, Encoder<StackTraceElement[]> {

        /**
         * {@inheritDoc}
         */
        @Override
        public String encode(StackTraceElement[] values) {
            Encoder<StackTraceElement> encoder = I.find(Encoder.class, StackTraceElement.class);

            StringJoiner joiner = new StringJoiner(",");
            for (StackTraceElement value : values) {
                joiner.add(encoder.encode(value));
            }
            return joiner.toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public StackTraceElement[] decode(String value) {
            Decoder<StackTraceElement> decoder = I.find(Decoder.class, StackTraceElement.class);
            String[] values = value.split(",");
            StackTraceElement[] elements = new StackTraceElement[values.length];

            for (int i = 0; i < elements.length; i++) {
                elements[i] = decoder.decode(values[i]);
            }
            return elements;
        }
    }
}
