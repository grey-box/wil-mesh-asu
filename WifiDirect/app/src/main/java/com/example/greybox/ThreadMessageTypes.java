package com.example.greybox;


public final class ThreadMessageTypes {
    // Using constants instead of enums to avoid using the `ordinal()` or adding too much code since
    // we require to use them as `int`
    public static final int MESSAGE_READ = 0;
    public static final int SOCKET_DISCONNECTION = 1;
    public static final int MESSAGE_WRITTEN = 2;
    public static final int HANDLE = 3;
    public static final int CLIENT_SOCKET_CONNECTION = 4;
}
