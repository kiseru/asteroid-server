package com.tutorteam;

import com.tutorteam.server.Server;

import java.io.IOException;

final public class ApplicationRunner {

    public static void main(String[] args) throws IOException {
        Server server = new Server(6501);
        server.up();
    }
}
