/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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

import bee.Bee;
import bee.Platform;
import bee.UserInterface;
import bee.api.Command;
import bee.api.Library;
import kiss.I;
import psychopath.Directory;
import psychopath.Location;
import psychopath.Locator;

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
    private Directory directory;

    /** {@link System#out} and {@link System#in} encoding. */
    private Charset encoding;

    /** The execution type. */
    private boolean sync = true;

    /** The execution type. */
    private boolean headless = true;

    /** Xms option. */
    private int initialMemory = 256;

    /** Xmx option. */
    private int maxMemory = 2048;

    /** The system properties. */
    private final List<String> properties = new ArrayList();

    /**
     * Hide Constructor.
     */
    private Java() {
    }

    /**
     * Create JVM instance.
     */
    public static Java with() {
        return new Java();
    }

    /**
     * Add classpath.
     * 
     * @param paths
     */
    public Java classPath(Collection<Library> paths) {
        if (paths != null) {
            for (Library library : paths) {
                classpaths.add(library.getLocalJar().asJavaPath());
            }
        }

        // API definition
        return this;
    }

    /**
     * Add classpath.
     * 
     * @param path
     */
    public Java classPath(Location path) {
        if (path != null) {
            classpaths.add(path.absolutize().asJavaPath());
        }

        // API definition
        return this;
    }

    /**
     * Add classpath.
     * 
     * @param classes
     */
    public Java classPath(Class... classes) {
        if (classes != null) {
            for (Class clazz : classes) {
                classPath(Locator.locate(clazz));
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
     * Enable assertion functionality.
     */
    public Java enableAssertion() {
        enableAssertion = true;

        // API definition
        return this;
    }

    /**
     * Make this process running asynchronously.
     * 
     * @return Fluent API.
     */
    public Java inParallel(boolean enable) {
        this.sync = !enable;

        // API definition
        return this;
    }

    /**
     * Make this process running in headless environment.
     * 
     * @return Fluent API.
     */
    public Java inHeadless(boolean enable) {
        this.headless = enable;

        // API definition
        return this;
    }

    /**
     * Set working directory.
     * 
     * @param directory A location of working directory.
     */
    public Java workingDirectory(Directory directory) {
        this.directory = directory;

        // API definition
        return this;
    }

    /**
     * Set {@link System#out} and {@link System#in} encoding.
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
     * Set system property for this JVM.
     * 
     * @param key A property key.
     * @param value A property value.
     * @return
     */
    public Java systemProperty(String key, Object value) {
        if (key != null && key.length() != 0 && value != null) {
            properties.add("-D" + key + "=\"" + value + "\"");
        }

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
        if (headless && Platform.isLinux()) {
            command.add("xvfb-run");
            command.add("--auto-servernum");
        }

        command.add("java");

        if (classpaths.size() != 0) {
            command.add("-cp");
            command.add(classpaths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator)));
        }

        if (enableAssertion) {
            command.add("-ea");
        }

        for (String property : properties) {
            command.add(property);
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

        System.out.println(command);

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
                try {
                    e.printStackTrace(new PrintWriter(Locator.file("external-java-process-error.log").asJavaFile()));
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
     * 
     */
    public static abstract class JVM {

        /** The user interface of the parent process. */
        protected final UserInterface ui = new JVMUserInterface();

        /** The activation argument. */
        protected String[] args;

        /**
         * Write sub-process code.
         * 
         * @throws Exception Execution error.
         */
        protected abstract void process() throws Exception;

        /**
         * Main method is activation point of sub process JVM.
         */
        public static void main(String[] args) throws Exception {
            // load stacktrace codec
            I.load(Bee.class);

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
                e.printStackTrace();
                vm.ui.error(e);
            } finally {
                System.exit(0);
            }
        }

        /**
         * This class is {@link UserInterface} wrapper of {@link Transporter} for interprocess
         * communication.
         * <p>
         * Must be non-static class to hide from class scanning.
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
            protected void write(int type, String message) {
                switch (type) {
                case TRACE:
                    transporter.trace(message);
                    break;

                case DEBUG:
                    transporter.debug(message);
                    break;

                case INFO:
                    transporter.info(message);
                    break;

                case WARNING:
                    transporter.warn(message);
                    break;

                case ERROR:
                    transporter.error(message);
                    break;

                default:
                    break;
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void write(Throwable error) {
                try {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    ObjectOutputStream out = new ObjectOutputStream(bytes);
                    out.writeObject(error);
                    out.close();

                    transporter.object(bytes.toByteArray());
                } catch (Exception ex) {
                    throw I.quiet(ex);
                }
            }
        }
    }

    /**
     * Transporter between parent process and sub process.
     */
    @MXBean
    public static interface Transporter {

        /**
         * Talk to user.
         * 
         * @param message
         */
        void title(String message);

        /**
         * Talk to user.
         * 
         * @param message
         */
        void trace(String message);

        /**
         * Talk to user.
         * 
         * @param message
         */
        void debug(String message);

        /**
         * Talk to user.
         * 
         * @param message
         */
        void info(String message);

        /**
         * Talk to user.
         * 
         * @param message
         */
        void warn(String message);

        /**
         * Error message
         * 
         * @param error
         */
        void error(String error);

        /**
         * Send the serialized object.
         * 
         * @param bytes
         */
        void object(byte[] bytes);
    }

    /**
     * @version 2018/03/29 22:48:25
     */
    static class JVMTransporter extends StandardMBean implements Transporter {

        /** The actual user interface. */
        private final UserInterface ui;

        /** The sub process state. */
        private Throwable error;

        /**
         * Listen sub process event.
         */
        private JVMTransporter(UserInterface ui) {
            super(Transporter.class, false);

            this.ui = ui;
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
        public void trace(String message) {
            ui.trace(message);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void debug(String message) {
            ui.debug(message);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void info(String message) {
            ui.info(message);
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
            ui.error(error);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void object(byte[] bytes) {
            try {
                Object o = new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
                if (o instanceof Throwable e) {
                    error = e;
                } else {
                    ui.info(o);
                }
            } catch (Exception e) {
                throw I.quiet(e);
            }
        }
    }
}