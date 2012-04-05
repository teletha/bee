/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.tool;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MXBean;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import kiss.I;
import kiss.model.Codec;
import bee.UserInterface;
import bee.definition.Library;
import bee.util.NetworkAddressUtil;
import bee.util.ProcessMaker;

/**
 * @version 2012/04/04 17:08:12
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

    /**
     * <p>
     * Add classpath.
     * </p>
     * 
     * @param path
     */
    public void addClassPath(Collection<Library> paths) {
        if (paths != null) {
            for (Library library : paths) {
                classpaths.add(library.getJar());
            }
        }
    }

    /**
     * <p>
     * Add classpath.
     * </p>
     * 
     * @param path
     */
    public void addClassPath(Path path) {
        if (path != null) {
            classpaths.add(path.toAbsolutePath());
        }
    }

    /**
     * <p>
     * Enable assertion functionality.
     * </p>
     */
    public void enableAssertion() {
        enableAssertion = true;
    }

    /**
     * <p>
     * Set working directory.
     * </p>
     * 
     * @param directory A location of working directory.
     */
    public void setWorkingDirectory(Path directory) {
        this.directory = directory;
    }

    /**
     * <p>
     * Execute this java command.
     * </p>
     */
    public void run(Class<? extends SubProcess> mainClass, Object... arguments) {
        // create address
        int port = NetworkAddressUtil.getPort();
        String address = "service:jmx:rmi:///jndi/rmi://localhost:" + port + "/hello";

        List<String> command = new ArrayList();
        command.add("java");

        if (classpaths.size() != 0) {
            command.add("-cp");
            command.add(I.join(classpaths, File.pathSeparator));
        }

        if (enableAssertion) {
            command.add("-ea");
        }

        command.add(SubProcessActivator.class.getName());
        command.add(address);
        command.add(mainClass.getName());

        for (Object arg : arguments) {
            command.add(arg.toString());
        }

        try {
            LocateRegistry.createRegistry(port);
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            server.registerMBean(I.make(Transporter.class), NAME);

            JMXConnectorServer connector = JMXConnectorServerFactory.newJMXConnectorServer(new JMXServiceURL(address), null, server);
            connector.start();

            // build sub-process for java
            ProcessMaker maker = new ProcessMaker();
            maker.setWorkingDirectory(directory);
            maker.run(command);

            connector.stop();
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * @version 2012/04/05 1:50:00
     */
    public static abstract class SubProcess {

        /** The user interface of the parent process. */
        protected UserInterface ui;

        /** The activation argument. */
        protected String[] args;

        /**
         * <p>
         * Write sub-process code.
         * </p>
         * 
         * @param transporter
         */
        protected abstract void process();
    }

    /**
     * @version 2012/04/04 21:27:43
     */
    @SuppressWarnings("unused")
    private static final class SubProcessActivator {

        /**
         * @param args
         */
        public static void main(String[] args) throws Exception {
            // launch observer
            JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(args[0]));
            MBeanServerConnection connection = connector.getMBeanServerConnection();

            SubProcess process = (SubProcess) I.make(Class.forName(args[1]));
            process.ui = new SubProcessUserInterface(MBeanServerInvocationHandler.newProxyInstance(connection, NAME, Transporter.class, false));
            process.args = Arrays.copyOfRange(args, 2, args.length);
            process.process();

            connector.close();
        }
    }

    /**
     * @version 2012/04/05 1:55:23
     */
    private static final class SubProcessUserInterface extends UserInterface {

        /** The event transporter. */
        private final Transporter transporter;

        /**
         * <p>
         * Remote UserInterface.
         * </p>
         */
        private SubProcessUserInterface(Transporter transporter) {
            this.transporter = transporter;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String ask(String question) {
            // If this exception will be thrown, it is bug of this program. So we must rethrow the
            // wrapped error in here.
            throw new Error();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T ask(String question, T defaultAnswer) {
            // If this exception will be thrown, it is bug of this program. So we must rethrow the
            // wrapped error in here.
            throw new Error();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T ask(Class<T> question) {
            // If this exception will be thrown, it is bug of this program. So we must rethrow the
            // wrapped error in here.
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
            StringBuilder builder = new StringBuilder();

            for (Object message : messages) {
                builder.append(message.toString());
            }
            transporter.talk(builder.toString());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void warn(Object... messages) {
            StringBuilder builder = new StringBuilder();

            for (Object message : messages) {
                builder.append(message.toString());
            }
            transporter.warn(builder.toString());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public RuntimeException error(Object... messages) {
            for (Object message : messages) {
                if (message instanceof Throwable) {
                    StringBuilder builder = new StringBuilder();
                    Cause cause = make((Throwable) message);
                    I.write(cause, builder, false);

                    transporter.error(builder.toString());

                    return I.quiet(message);
                }
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void write(String message) {
            // If this exception will be thrown, it is bug of this program. So we must rethrow the
            // wrapped error in here.
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
        private static Cause make(Throwable throwable) {
            Cause cause = new Cause();
            cause.className = throwable.getClass().getName();
            cause.message = throwable.getMessage();
            if (throwable.getCause() != null) cause.cause = make(throwable.getCause());
            for (StackTraceElement element : throwable.getStackTrace()) {
                cause.traces.add(element);
            }

            // API definition
            return cause;
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
    protected static interface Transporter {

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
     * @version 2012/04/05 0:56:07
     */
    @SuppressWarnings("unused")
    private static final class Listener implements Transporter {

        /** The actual user interface. */
        private final UserInterface ui;

        /**
         * Listen sub process event.
         */
        private Listener(UserInterface ui) {
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
            try {
                Cause cause = I.read(error, new Cause());
                Error e = new Error(cause.message);
                e.setStackTrace(cause.traces.toArray(new StackTraceElement[cause.traces.size()]));

                ui.error(e);
            } catch (Exception e) {
                throw I.quiet(e);
            }
        }
    }

    /**
     * @version 2012/04/06 0:30:59
     */
    public static final class Cause {

        /** The error class name. */
        public String className;

        /** The error message. */
        public String message;

        /** The cause. */
        public Cause cause;

        /** The stack trace. */
        public List<StackTraceElement> traces = new ArrayList();
    }

    /**
     * @version 2012/04/06 0:33:18
     */
    private static final class StackTraceCodec extends Codec<StackTraceElement> {

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
}
