package ru.ifmo.rain.lemeshkova.bank.tests;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class BankTests {

    public static void main(final String[] args) {
        final TestExecutionSummary summary = runTests();
//		summary.printTo(new PrintWriter(System.out));
        System.exit(summary.getTestsFailedCount() == 0 ? 0 : 1);
    }

    private static TestExecutionSummary runTests() {
        final SummaryGeneratingListener listener = new SummaryGeneratingListener();
        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(ClientTests.class))
                .selectors(selectClass(ServerTests.class))
                .build();
        final Launcher launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);
        return listener.getSummary();
    }
}
