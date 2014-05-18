package io.aie_btc_service.aie_btc_service.transaction;

import com.google.bitcoin.core.ECKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class TheUserKeys {

    private UserKeys tested;

    @Mock
    private ECKey ecKeyMock;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldReturnValidPublicKey() {

    }

    @Test
    public void shouldReturnOpenOutput() {

    }

    @Test
    public void shouldReturnIdOfUserKeys() {

    }
}
