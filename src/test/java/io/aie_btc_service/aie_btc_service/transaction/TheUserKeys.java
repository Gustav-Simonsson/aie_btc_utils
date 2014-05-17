package io.aie_btc_service.aie_btc_service.transaction;

import com.google.bitcoin.core.ECKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class TheUserKeys {

    private UserKeys tested;

    @Mock
    private ECKey ecKeyMock;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        tested = new UserKeys("some_name", "92r2FtYSQcqQMgzoXs3AzDAtu7Q3hgXmRD2HpcDM7g7UgArcxq6", null);
    }

    @Test
    public void shouldReturnValidPublicKey() {

        //byte[] key = [B@29b00e45.getBytes();
        //when(ecKeyMock.getPubKey()).thenReturn(key);

        assertEquals(tested.getPubKey(), ecKeyMock.getPubKey());
    }
}
