package fr.paris.lutece.plugins.captcha.modules.captchetat.web;

import fr.paris.lutece.plugins.captcha.modules.captchetat.business.CaptchEtatData;
import fr.paris.lutece.plugins.captcha.modules.captchetat.service.CaptchEtatService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.xpage.MVCApplication;
import fr.paris.lutece.portal.util.mvc.xpage.annotations.Controller;
import fr.paris.lutece.portal.web.xpages.XPage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

/**
 * This class manages CaptchEtat XPage interactions.
 */
@RequestScoped
@Named( "captchetat.xpage.captchetat" )
@Controller( xpageName = "captchetat", pageTitleI18nKey = "module.captcha.captchetat.pageTitle", pagePathI18nKey = "module.captcha.captchetat.pagePath" )
public class CaptchEtatXPage extends MVCApplication
{
    private static final long serialVersionUID = 1L;

    private static final String ACTION_REFRESH_CAPTCHA = "refreshCaptcha";

    @Inject
    private CaptchEtatService _captchEtatService;

    @Action( ACTION_REFRESH_CAPTCHA )
    public XPage refreshCaptcha( HttpServletRequest request )
    {
        try
        {
            CaptchEtatData data = _captchEtatService.generateCaptchaData( );

            if ( data != null )
            {
                ObjectMapper mapper = new ObjectMapper( );
                ObjectNode json = mapper.createObjectNode( );

                json.put( "uuid", data.getUuid( ) );
                json.put( "image", data.getImageBase64( ) );

                if ( data.getAudioBase64( ) != null )
                {
                    json.put( "audio", data.getAudioBase64( ) );
                }
                else
                {
                    json.putNull( "audio" );
                }

                return responseJSON( json.toString( ) );
            }
            else
            {
                return responseJSON( "{\"error\": \"Impossible de récupérer les données Captcha\"}" );
            }
        }
        catch( Exception e )
        {
            AppLogService.error( "Error refreshing CaptchEtat via XPage", e );
            return responseJSON( "{\"error\": \"Erreur interne serveur\"}" );
        }
    }
}
