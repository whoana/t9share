package rose.mary.trace.database.service; 

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import pep.per.mint.common.util.Util;
import rose.mary.trace.core.data.common.State;
 

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BotServiceTest {
    static {
        System.setProperty("rose.mary.home", "/Users/whoana/DEV/workspace-vs-refactoring/t9/home");
    }
    @Autowired
    StateService stateService;
    
    @Autowired
    BotService botService;

    @Test
    public void testGetState() throws Exception{
        State state = stateService.getState("TEST001", "20220830110214830830", "host1");
        System.out.println("state:" + Util.toJSONPrettyString(state));
        Assert.assertNotNull(state);
    }
}
