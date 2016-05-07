
import play.mvc.*;
import play.test.*;
import play.libs.F.*;

import java.util.function.Consumer;

import static play.test.Helpers.*;

import static org.fluentlenium.core.filter.FilterConstructor.*;

public class IntegrationTest {



    public void test() {
        running(testServer(3333, fakeApplication(inMemoryDatabase())), HTMLUNIT, new Consumer<TestBrowser>() {
            public void accept(TestBrowser browser) {
                browser.goTo("http://localhost:3333");
                //Assert.assertTrue(browser.pageSource().contains("Your new application is ready."));
            }
        });
    }

}
