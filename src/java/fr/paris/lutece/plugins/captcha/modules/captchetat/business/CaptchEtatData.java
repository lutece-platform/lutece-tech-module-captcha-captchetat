package fr.paris.lutece.plugins.captcha.modules.captchetat.business;


public class CaptchEtatData
{
    private String _strUuid;
    private String _strImageBase64;
    private String _strAudioBase64;

    public String getUuid( )
    {
        return _strUuid;
    }

    public void setUuid( String strUuid )
    {
        _strUuid = strUuid;
    }

    public String getImageBase64( )
    {
        return _strImageBase64;
    }

    public void setImageBase64( String strImageBase64 )
    {
        _strImageBase64 = strImageBase64;
    }

    public String getAudioBase64( )
    {
        return _strAudioBase64;
    }

    public void setAudioBase64( String strAudioBase64 )
    {
        _strAudioBase64 = strAudioBase64;
    }
}
