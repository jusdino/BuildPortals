import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

public class TestMockBukkit {
    private ServerMock server;

    @BeforeEach
    public void setUp()
    {
        server = MockBukkit.mock();
    }

    @AfterEach
    public void tearDown()
    {
        MockBukkit.unmock();
    }

    @Test
    void testAddPlayer() {
        try {
            server.addPlayer();
        } catch (NoSuchMethodError e) {
            e.printStackTrace();
            throw e;
        }
    }
}
