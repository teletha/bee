/*
 * Copyright (C) 2026 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import bee.Platform;

/**
 * A low level terminal controller which switches the console between the canonical (line) mode and
 * the raw mode. In the raw mode the arrow keys are delivered to the program immediately, so the
 * user can navigate a list without pushing the enter key.
 * <p>
 * This class uses the FFM API to call the platform native function directly.
 * </p>
 */
public class Terminal implements AutoCloseable {

    /** STD_INPUT_HANDLE. */
    private static final int STD_INPUT_HANDLE = -10;

    /** ENABLE_PROCESSED_INPUT (the control character is processed by the system). */
    private static final int ENABLE_PROCESSED_INPUT = 0x0001;

    /** ENABLE_LINE_INPUT (the system reads a line at once). */
    private static final int ENABLE_LINE_INPUT = 0x0002;

    /** ENABLE_ECHO_INPUT (the system echoes the input). */
    private static final int ENABLE_ECHO_INPUT = 0x0004;

    /** ENABLE_VIRTUAL_TERMINAL_INPUT (the system converts the arrow keys to escape sequences). */
    private static final int ENABLE_VIRTUAL_TERMINAL_INPUT = 0x0200;

    /** ICANON (the canonical mode flag on Linux). */
    private static final int LINUX_ICANON = 0x0002;

    /** ECHO (the echo flag on Linux). */
    private static final int LINUX_ECHO = 0x0008;

    /** ICANON (the canonical mode flag on macOS and BSD). */
    private static final int BSD_ICANON = 0x0100;

    /** ECHO (the echo flag on macOS and BSD). */
    private static final int BSD_ECHO = 0x0008;

    /** The offset of the c_lflag field in the termios on Linux. */
    private static final long LINUX_LFLAG = 12;

    /** The offset of the c_lflag field in the termios on macOS and BSD. */
    private static final long BSD_LFLAG = 24;

    /** A generous buffer which can hold any termios layout. */
    private static final long TERMIOS_SIZE = 128;

    /** The raw mode is active or not. */
    private boolean enabled;

    /** The arena for the native memory. */
    private Arena arena;

    /** The saved console mode or the saved termios. */
    private MemorySegment saved;

    /** The native SetConsoleMode function. */
    private MethodHandle setConsoleMode;

    /** The native tcsetattr function. */
    private MethodHandle tcsetattr;

    /** The console handle on Windows. */
    private long handle;

    /**
     * Enter the raw mode. If the terminal can not be switched to the raw mode, the returned
     * instance is disabled and {@link #isEnabled()} returns <code>false</code>.
     * 
     * @return A terminal controller.
     */
    public static Terminal raw() {
        Terminal terminal = new Terminal();

        try {
            if (Platform.isWindows()) {
                terminal.enableWindows();
            } else {
                terminal.enableUnix();
            }
            terminal.enabled = true;
            Runtime.getRuntime().addShutdownHook(new Thread(terminal::restore));
        } catch (Throwable error) {
            terminal.enabled = false;
            terminal.release();
        }
        return terminal;
    }

    /**
     * Hide the constructor.
     */
    protected Terminal() {
    }

    /**
     * Switch the Windows console to the raw mode.
     * 
     * @throws Throwable If the native call fails.
     */
    private void enableWindows() throws Throwable {
        arena = Arena.ofShared();

        SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32", arena);
        MethodHandle getStdHandle = Linker.nativeLinker()
                .downcallHandle(kernel32.find("GetStdHandle").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
        MethodHandle getConsoleMode = Linker.nativeLinker()
                .downcallHandle(kernel32.find("GetConsoleMode").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
        setConsoleMode = Linker.nativeLinker()
                .downcallHandle(kernel32.find("SetConsoleMode").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));

        handle = (long) getStdHandle.invoke(STD_INPUT_HANDLE);
        saved = arena.allocate(ValueLayout.JAVA_INT);

        if ((int) getConsoleMode.invoke(handle, saved.address()) == 0) {
            throw new IllegalStateException("GetConsoleMode");
        }

        int mode = saved.get(ValueLayout.JAVA_INT, 0);
        int raw = (mode & ~(ENABLE_LINE_INPUT | ENABLE_ECHO_INPUT)) | ENABLE_VIRTUAL_TERMINAL_INPUT | ENABLE_PROCESSED_INPUT;

        if ((int) setConsoleMode.invoke(handle, raw) == 0) {
            throw new IllegalStateException("SetConsoleMode");
        }
    }

    /**
     * Switch the Unix terminal to the raw mode.
     * 
     * @throws Throwable If the native call fails.
     */
    private void enableUnix() throws Throwable {
        arena = Arena.ofShared();

        Linker linker = Linker.nativeLinker();
        SymbolLookup libc = SymbolLookup.libraryLookup("c", arena);
        MethodHandle tcgetattr = linker.downcallHandle(libc.find("tcgetattr").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
        tcsetattr = linker.downcallHandle(libc.find("tcsetattr").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));

        saved = arena.allocate(TERMIOS_SIZE);

        if ((int) tcgetattr.invoke(0, saved.address()) != 0) {
            throw new IllegalStateException("tcgetattr");
        }

        long offset = Platform.isMac() ? BSD_LFLAG : LINUX_LFLAG;
        int mask = Platform.isMac() ? BSD_ICANON | BSD_ECHO : LINUX_ICANON | LINUX_ECHO;
        MemorySegment copy = arena.allocate(TERMIOS_SIZE);
        copy.copyFrom(saved);
        copy.set(ValueLayout.JAVA_INT, offset, copy.get(ValueLayout.JAVA_INT, offset) & ~mask);

        // TCSANOW
        if ((int) tcsetattr.invoke(0, 0, copy.address()) != 0) {
            throw new IllegalStateException("tcsetattr");
        }
    }

    /**
     * Check whether the raw mode is active or not.
     * 
     * @return A result.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Read one byte from the console. This method blocks until a byte is available.
     * 
     * @return A read byte, or <code>-1</code> when the input is closed.
     */
    public int read() {
        try {
            return System.in.read();
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Restore the original console mode.
     */
    public synchronized void restore() {
        if (!enabled) {
            return;
        }
        enabled = false;

        try {
            if (Platform.isWindows()) {
                setConsoleMode.invoke(handle, saved.get(ValueLayout.JAVA_INT, 0));
            } else {
                tcsetattr.invoke(0, 0, saved.address());
            }
        } catch (Throwable error) {
            // ignore
        }
        release();
    }

    /**
     * Release the native memory.
     */
    private void release() {
        if (arena != null) {
            try {
                arena.close();
            } catch (Throwable error) {
                // ignore
            }
            arena = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        restore();
    }
}
