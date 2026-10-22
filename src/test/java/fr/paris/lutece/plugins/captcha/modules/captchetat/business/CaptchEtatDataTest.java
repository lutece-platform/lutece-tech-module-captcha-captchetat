package fr.paris.lutece.plugins.captcha.modules.captchetat.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * CaptchEtatData Test
 */
public class CaptchEtatDataTest
{
    @Test
    public void testGettersAndSetters( )
    {
        CaptchEtatData data = new CaptchEtatData( );
        assertNull( data.getUuid( ) );
        assertNull( data.getImageBase64( ) );
        assertNull( data.getAudioBase64( ) );

        data.setUuid( "uuid" );
        data.setImageBase64( "data:image/png;base64,AAAA" );
        data.setAudioBase64( "data:audio/wav;base64,BBBB" );

        assertEquals( "uuid", data.getUuid( ) );
        assertEquals( "data:image/png;base64,AAAA", data.getImageBase64( ) );
        assertEquals( "data:audio/wav;base64,BBBB", data.getAudioBase64( ) );
    }
}
