package fr._42.marchepublic.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.time.Duration;

@Configuration
public class SeleniumConfig {

    @Bean
    @ConditionalOnProperty(prefix = "scraper.selenium", name = "enabled", havingValue = "true")
    public ChromeOptions chromeOptions(
            @Value("${scraper.selenium.headless:true}") boolean headless) {
        ChromeOptions options = new ChromeOptions();

        if (headless) {
            options.addArguments("--headless=new");
        }

        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        return options;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @ConditionalOnProperty(prefix = "scraper.selenium", name = "enabled", havingValue = "true")
    public WebDriver webDriver(ObjectProvider<ChromeOptions> chromeOptionsProvider,
                               @Value("${scraper.selenium.implicit-wait-seconds:0}") long implicitWaitSeconds,
                               @Value("${scraper.selenium.page-load-timeout-seconds:30}") long pageLoadTimeoutSeconds,
                               @Value("${scraper.selenium.script-timeout-seconds:30}") long scriptTimeoutSeconds) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = chromeOptionsProvider.getIfAvailable(ChromeOptions::new);
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(Math.max(implicitWaitSeconds, 0)));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(Math.max(pageLoadTimeoutSeconds, 1)));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(Math.max(scriptTimeoutSeconds, 1)));

        return driver;
    }
}
