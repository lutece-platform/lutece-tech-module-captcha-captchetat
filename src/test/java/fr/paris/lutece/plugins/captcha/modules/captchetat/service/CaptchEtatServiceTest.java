package fr.paris.lutece.plugins.captcha.modules.captchetat.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * CaptchEtatService Test (cases that do not call the CaptchEtat API)
 */
public class CaptchEtatServiceTest
{
    private static final String PARAMETER_UUID = "captchetat_uuid";
    private static final String PARAMETER_CODE = "captchetat_answer";

    private final CaptchEtatService _service = new CaptchEtatService( );

    @Test
    public void testGetCaptchaEngineName( )
    {
        assertEquals( "CaptchEtat", _service.getCaptchaEngineName( ) );
    }

    @Test
    public void testValidateWithoutParameters( )
    {
        assertFalse( _service.validate( new MockHttpServletRequest( ) ) );
    }

    @Test
    public void testValidateWithMissingParameter( )
    {
        MockHttpServletRequest request = new MockHttpServletRequest( );
        request.addParameter( PARAMETER_UUID, "uuid" );
        assertFalse( _service.validate( request ) );

        request = new MockHttpServletRequest( );
        request.addParameter( PARAMETER_CODE, "code" );
        assertFalse( _service.validate( request ) );
    }

    @Test
    public void testValidateApiWithEmptyValues( )
    {
        assertFalse( _service.validateApi( null, "code" ) );
        assertFalse( _service.validateApi( "uuid", null ) );
        assertFalse( _service.validateApi( "", "code" ) );
        assertFalse( _service.validateApi( "uuid", "" ) );
    }
}
