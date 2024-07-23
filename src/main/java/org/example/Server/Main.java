package org.example.Server;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        if(args.length<1){
            System.out.println("wrong parameters, use key -h to help, command \"@exit\" to exit after joining, \ncommand \"@getMembers\" to get list of current members");
            return;
        }
        try {
            String tmp=args[0];
            if(tmp.equals("-h")){
                getHelp();
                return;
            }
            int port=getPort(tmp);

            Server server = new Server("localhost", port);
            server.run();
        } catch (IOException | ServerException e) {
            System.out.println("server cannot open or crush");
        }
    }
    private static int getPort(String filePath){
        File file = new File(filePath);
        try(Scanner scanner = new Scanner(file)){
            int port=-1;
            while (scanner.hasNextInt()) {
                port = scanner.nextInt();
            }
            scanner.close();
            return port;
        } catch (Exception ex) {
            System.out.println("wrong config file");
            System.exit(0);
        }
        return 0;
    }

    private static void getHelp(){
        System.out.println("u have to enter program name and name of config file with port number");
    }
}
