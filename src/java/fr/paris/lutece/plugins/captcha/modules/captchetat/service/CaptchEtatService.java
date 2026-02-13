package fr.paris.lutece.plugins.captcha.modules.captchetat.service;

import fr.paris.lutece.plugins.captcha.modules.captchetat.business.CaptchEtatData;
import fr.paris.lutece.plugins.captcha.service.ICaptchaEngine;
import fr.paris.lutece.portal.service.template.AppTemplateService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.html.HtmlTemplate;
import fr.paris.lutece.util.httpaccess.HttpAccess;
import fr.paris.lutece.util.httpaccess.HttpAccessException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Named( "captchEtatService" )
@ApplicationScoped
public class CaptchEtatService implements ICaptchaEngine
{
    private static final String TEMPLATE_CAPTCHA = "skin/plugins/captcha/modules/captchetat/captchetat.html";

    private static final String PROPERTY_TOKEN_URL = "captchetat.api.token.url";
    private static final String PROPERTY_API_URL = "captchetat.api.url";
    private static final String PROPERTY_CLIENT_ID = "captchetat.client.id";
    private static final String PROPERTY_CLIENT_SECRET = "captchetat.client.secret";
    private static final String PROPERTY_GRANT_TYPE = "captchetat.api.grant_type";
    private static final String PROPERTY_SCOPE = "captchetat.api.scope";
    private static final String PROPERTY_PATH_VALIDATE = "captchetat.api.path.validate";
    private static final String PROPERTY_PATH_ENDPOINT = "captchetat.api.path.endpoint";
    private static final String PROPERTY_CAPTCHA_CONTEXT = "captchetat.api.context";

    private static final String CAPTCHA_PROVIDER = "CaptchEtat";
    private static final String DEFAULT_PATH_ENDPOINT = "/simple-captcha-endpoint";
    private static final String DEFAULT_PATH_VALIDATE = "/valider-captcha";
    private static final String DEFAULT_GRANT_TYPE = "client_credentials";
    private static final String DEFAULT_CAPTCHA_CONTEXT = "captchaFR";
    private static final String DEFAULT_SCOPE = "piste.captchetat";

    private static final String API_KEY_ACCESS_TOKEN = "access_token";
    private static final String API_KEY_EXPIRES_IN = "expires_in";
    private static final String UUID = "uuid";
    private static final String CODE = "code";
    private static final String IMAGEB_64 = "imageb64";

    private static final String PREFIX_BEARER = "Bearer ";
    private static final String MIME_TYPE_AUDIO_WAV_X_WAV = "audio/x-wav, audio/wav";
    private static final String PNG_BASE_64 = "data:image/png;base64,";
    private static final String AUDIO_WAV_BASE_64 = "data:audio/wav;base64,";

    private static final String PARAMETER_UUID = "captchetat_uuid";
    private static final String PARAMETER_CODE = "captchetat_answer";
    private static final String PARAMETER_IMAGE_DATA = "captchetat_image_data";
    private static final String PARAMETER_SOUND_DATA = "captchetat_sound_data";

    private static final String PARAM_GRANT_TYPE = "grant_type";
    private static final String PARAM_CLIENT_ID = "client_id";
    private static final String PARAM_CLIENT_SECRET = "client_secret";
    private static final String PARAM_SCOPE = "scope";


    private String _strAccessToken;
    private long _lTokenExpirationTime;

    @Override
    public String getCaptchaEngineName( )
    {
        return CAPTCHA_PROVIDER;
    }

    @Override
    public boolean validate( HttpServletRequest request )
    {
        String strUuid = request.getParameter( PARAMETER_UUID );
        String strCode = request.getParameter( PARAMETER_CODE );

        if ( strUuid == null || strCode == null )
        {
            return false;
        }

        return validateApi( strUuid, strCode );
    }

    public CaptchEtatData generateCaptchaData( )
    {
        CloseableHttpClient httpClient = null;
        try
        {
            String strToken = getAccessToken( );
            String strApiBaseUrl = AppPropertiesService.getProperty( PROPERTY_API_URL ) +
                    AppPropertiesService.getProperty( PROPERTY_PATH_ENDPOINT, DEFAULT_PATH_ENDPOINT );
            String strContext = AppPropertiesService.getProperty( PROPERTY_CAPTCHA_CONTEXT, DEFAULT_CAPTCHA_CONTEXT );

            httpClient = HttpClients.createDefault( );

            String strUrlImg = buildCaptchaUrl( strApiBaseUrl, strContext, "image", null );
            JsonNode strCaptchaResponse = getCaptchaIMG( httpClient, strUrlImg, strToken );

            if ( strCaptchaResponse != null )
            {
                CaptchEtatData data = new CaptchEtatData( );
                String strUuid = strCaptchaResponse.path( UUID ).asText( );
                String strImageB64 = strCaptchaResponse.path( IMAGEB_64 ).asText( );

                data.setUuid( strUuid );
                data.setImageBase64( strImageB64 );

                String strUrlSound = buildCaptchaUrl( strApiBaseUrl, strContext, "sound", strUuid );
                byte[] soundBytes = getCaptchaSound( httpClient, strUrlSound, strToken );

                if ( soundBytes != null )
                {
                    String strSoundB64 = Base64.getEncoder( ).encodeToString( soundBytes );
                    data.setAudioBase64( AUDIO_WAV_BASE_64 + strSoundB64 );
                }

                return data;
            }
        }
        catch ( Exception e )
        {
            AppLogService.error( "CaptchEtat: Technical error during data generation", e );
        }
        finally
        {
            closeResources( httpClient, null );
        }

        return null;
    }

    @Override
    public String getHtmlCode( )
    {
        Map<String, Object> model = new HashMap<>( );

        CaptchEtatData data = generateCaptchaData( );

        if ( data != null )
        {
            model.put( PARAMETER_UUID, data.getUuid( ) );
            model.put( PARAMETER_IMAGE_DATA, data.getImageBase64( ) );

            if ( data.getAudioBase64( ) != null )
            {
                model.put( PARAMETER_SOUND_DATA, data.getAudioBase64( ) );
            }
        }
        else
        {
            AppLogService.error( "CaptchEtat: Failed to load captcha data." );
        }

        HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_CAPTCHA, Locale.getDefault( ), model );
        return template.getHtml( );
    }


    private String buildCaptchaUrl( String strBaseUrl, String strContext, String strType, String strUuid )
    {
        StringBuilder sbUrl = new StringBuilder( strBaseUrl );
        sbUrl.append( strBaseUrl.contains( "?" ) ? "&" : "?" );
        sbUrl.append( "get=" ).append( strType );
        sbUrl.append( "&c=" ).append( URLEncoder.encode( strContext, StandardCharsets.UTF_8 ) );

        if ( strUuid != null )
        {
            sbUrl.append( "&t=" ).append( URLEncoder.encode( strUuid, StandardCharsets.UTF_8 ) );
        }
        return sbUrl.toString( );
    }

    private JsonNode getCaptchaIMG( CloseableHttpClient client, String strUrl, String strToken )
    {
        HttpGet httpGet = new HttpGet( strUrl );
        httpGet.setHeader( HttpHeaders.AUTHORIZATION, PREFIX_BEARER + strToken );
        httpGet.setHeader( HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON );

        try ( CloseableHttpResponse response = client.execute( httpGet ) )
        {
            if ( response.getCode( ) == HttpStatus.SC_OK )
            {
                String strResponseJson = EntityUtils.toString( response.getEntity( ), StandardCharsets.UTF_8 );
                return new ObjectMapper( ).readTree( strResponseJson );
            }
            AppLogService.error( "CaptchEtat: HTTP Image Error: " + response.getCode( ) );
        }
        catch( Exception e )
        {
            AppLogService.error( "CaptchEtat: JSON call error", e );
        }
        return null;
    }

    private byte[] getCaptchaSound( CloseableHttpClient client, String strUrl, String strToken )
    {
        HttpGet httpGet = new HttpGet( strUrl );
        httpGet.setHeader( HttpHeaders.AUTHORIZATION, PREFIX_BEARER + strToken );
        httpGet.setHeader( HttpHeaders.ACCEPT, MIME_TYPE_AUDIO_WAV_X_WAV );

        try ( CloseableHttpResponse response = client.execute( httpGet ) )
        {
            if ( response.getCode( ) == HttpStatus.SC_OK )
            {
                return EntityUtils.toByteArray( response.getEntity( ) );
            }
            AppLogService.error( "CaptchEtat: HTTP Audio Error: " + response.getCode( ) );
        }
        catch( Exception e )
        {
            AppLogService.error( "CaptchEtat: Audio call error", e );
        }
        return null;
    }

    public synchronized String getAccessToken( ) throws HttpAccessException
    {
        if ( _strAccessToken != null && System.currentTimeMillis( ) < _lTokenExpirationTime )
        {
            return _strAccessToken;
        }

        HttpAccess httpAccess = new HttpAccess( );
        Map<String, String> params = new HashMap<>( );
        params.put( PARAM_GRANT_TYPE, AppPropertiesService.getProperty( PROPERTY_GRANT_TYPE, DEFAULT_GRANT_TYPE ) );
        params.put( PARAM_CLIENT_ID, AppPropertiesService.getProperty( PROPERTY_CLIENT_ID ) );
        params.put( PARAM_CLIENT_SECRET, AppPropertiesService.getProperty( PROPERTY_CLIENT_SECRET ) );
        params.put( PARAM_SCOPE, AppPropertiesService.getProperty( PROPERTY_SCOPE, DEFAULT_SCOPE ) );

        String strResponse = httpAccess.doPost( AppPropertiesService.getProperty( PROPERTY_TOKEN_URL ), params );

        try
        {
            ObjectMapper mapper = new ObjectMapper( );
            JsonNode root = mapper.readTree( strResponse );
            _strAccessToken = root.path( API_KEY_ACCESS_TOKEN ).asText( );
            long expiresIn = root.path( API_KEY_EXPIRES_IN ).asLong( );
            _lTokenExpirationTime = System.currentTimeMillis( ) + ( ( expiresIn - 60 ) * 1000 );

            AppLogService.info( "New CaptchEtat token generated successfully." );
        }
        catch( Exception e )
        {
            AppLogService.error( "Error parsing CaptchEtat token response", e );
            throw new HttpAccessException( "JSON Token Parsing Error", e );
        }

        return _strAccessToken;
    }

    public boolean validateApi( String strUuid, String strCode )
    {
        if ( strUuid == null || strCode == null || strUuid.isEmpty( ) || strCode.isEmpty( ) )
        {
            return false;
        }

        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;

        try
        {
            String strToken = getAccessToken( );
            String strPathValidate = AppPropertiesService.getProperty( PROPERTY_PATH_VALIDATE, DEFAULT_PATH_VALIDATE );

            String strUrl = AppPropertiesService.getProperty( PROPERTY_API_URL ) + strPathValidate;

            ObjectMapper mapper = new ObjectMapper( );
            ObjectNode jsonNode = mapper.createObjectNode( );
            jsonNode.put( UUID, strUuid );
            jsonNode.put( CODE, strCode );
            String strJsonBody = mapper.writeValueAsString( jsonNode );

            httpClient = HttpClients.createDefault( );
            HttpPost httpPost = new HttpPost( strUrl );

            httpPost.setHeader( HttpHeaders.AUTHORIZATION, PREFIX_BEARER + strToken );
            httpPost.setHeader( HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON );

            StringEntity entity = new StringEntity( strJsonBody, StandardCharsets.UTF_8 );
            httpPost.setEntity( entity );

            response = httpClient.execute( httpPost );
            int statusCode = response.getCode( );

            if ( statusCode == HttpStatus.SC_OK )
            {
                String strResponse = EntityUtils.toString( response.getEntity( ), StandardCharsets.UTF_8 );
                return "true".equalsIgnoreCase( strResponse.trim( ) ) || strResponse.contains( "\"success\":true" );
            }
            else
            {
                AppLogService.error( "CaptchEtat validation failed. HTTP Status: " + statusCode );
            }
        }
        catch( Exception e )
        {
            AppLogService.error( "Unexpected error during CaptchEtat validation", e );
        }
        finally
        {
            closeResources( httpClient, response );
        }

        return false;
    }

    private void closeResources( CloseableHttpClient client, CloseableHttpResponse response )
    {
        try
        {
            if ( response != null )
            {
                response.close( );
            }
            if ( client != null )
            {
                client.close( );
            }
        }
        catch( Exception e )
        {
            AppLogService.error( "Error closing HTTP resources: " + e.getMessage( ) );
        }
    }
}
