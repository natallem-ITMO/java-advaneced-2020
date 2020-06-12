package ru.ifmo.rain.lemeshkova.bank.server;

import ru.ifmo.rain.lemeshkova.bank.common.Bank;
import ru.ifmo.rain.lemeshkova.bank.common.Logger;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Server {
    private static final String UNIQUE_BINDING_NAME = "bank";
    private static final int port = 1099;
    private static final int bankPort = 8088;
    private static final String loggerSuffix = "{Server}";

    private static Registry registry;
    private static Bank bankService;

    private static void createRegistry() throws ServerBankException {
        try {
            registry = LocateRegistry.getRegistry(port);
            registry.list();// will throw an exception if the registry does not already exist
        } catch (RemoteException e) {
            try {
                registry = LocateRegistry.createRegistry(port);
            } catch (RemoteException remoteException) {
                throw new ServerBankException("Cannot create registry for bank server", e);
            }
        }
        Logger.info(loggerSuffix, "Created registry on port " + port);
    }

    public static void startServer() throws ServerBankException {
        createRegistry();
        bankService = new RemoteBank(port);
        Remote stub;
        try {
            stub = UnicastRemoteObject.exportObject(bankService, bankPort);
        } catch (RemoteException e) {
            throw new ServerBankException("Cannot create stub for bank service", e);
        }
        try {
            registry.rebind(UNIQUE_BINDING_NAME, stub);
        } catch (RemoteException e) {
            throw new ServerBankException("Cannot bind bank stup to registry ", e);
        }
        Logger.info(loggerSuffix, "Started server");
    }

    public static void main(String[] args) throws Exception {
        startServer();
    }

    public static void stop() throws ServerBankException {
        try {
            registry.unbind(UNIQUE_BINDING_NAME);
            UnicastRemoteObject.unexportObject(bankService, false);
            UnicastRemoteObject.unexportObject(registry, false);
        } catch (RemoteException | NotBoundException e) {
            throw new ServerBankException("Cannot unbind bank from local registry", e);
        }
        Logger.info(loggerSuffix, "Stopped server");
    }

}
