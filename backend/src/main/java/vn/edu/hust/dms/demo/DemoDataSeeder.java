package vn.edu.hust.dms.demo;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Runs the demo seed on startup when the "demo" profile is active. */
@Component
@Profile("demo")
public class DemoDataSeeder implements ApplicationRunner {

    private final DemoDataService demoData;

    public DemoDataSeeder(DemoDataService demoData) {
        this.demoData = demoData;
    }

    @Override
    public void run(ApplicationArguments args) {
        demoData.seedIfEmpty();
    }
}
