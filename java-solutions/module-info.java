

module java.solutions {
    requires java.compiler;

    requires info.kgeorgiy.java.advanced.arrayset;
    requires info.kgeorgiy.java.advanced.concurrent;
    requires info.kgeorgiy.java.advanced.crawler;
    requires info.kgeorgiy.java.advanced.hello;
    requires info.kgeorgiy.java.advanced.implementor;
    requires info.kgeorgiy.java.advanced.mapper;
    requires info.kgeorgiy.java.advanced.student;
    requires info.kgeorgiy.java.advanced.walk;
    requires java.rmi;

    exports ru.ifmo.rain.lemeshkova.hello;

    /*12 homework*/
  /*  requires java.rmi;

    exports ru.ifmo.rain.lemeshkova.bank.client;
    exports ru.ifmo.rain.lemeshkova.bank.server;
    exports ru.ifmo.rain.lemeshkova.bank.common;

    requires org.junit.jupiter;
    requires org.junit.platform.launcher;

    exports ru.ifmo.rain.lemeshkova.bank.tests;
    opens ru.ifmo.rain.lemeshkova.bank.tests to org.junit.jupiter, org.junit.platform.commons;*/
}