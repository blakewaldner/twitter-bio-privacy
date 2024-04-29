import twitter4j.*;
import com.mrpowergamerbr.temmiewebhook.*;
import com.mrpowergamerbr.temmiewebhook.embed.*;
import java.util.regex.*;
import java.util.*;

public class Discord
{
    private String color;
    private String footer;
    private String footerIcon;
    
    public Discord(final String color, final String footer, final String footerIcon) {
        this.color = color;
        this.footer = footer;
        this.footerIcon = footerIcon;
    }
    
    public void shortHook(final String hook, String message, final String profilePic, final String title, final URLEntity[] links) {
        final TemmieWebhook temmie = new TemmieWebhook(hook);
        final ThumbnailEmbed te = new ThumbnailEmbed();
        if (links != null) {
            message = this.replaceLinks(message, links);
        }
        te.setUrl(profilePic);
        final DiscordEmbed de = DiscordEmbed.builder().title(title).description(this.clickableTags(message)).thumbnail(te).footer(FooterEmbed.builder().text(this.footer).icon_url(this.footerIcon).build()).build();
        de.setColor(Integer.parseInt(this.color, 16));
        final DiscordMessage dm = DiscordMessage.builder().embeds(Arrays.asList(de)).build();
        temmie.sendMessage(dm);
    }
    
    public void header(final String hook, final String header, final String profilePic, final String title, final String account) {
        final TemmieWebhook temmie = new TemmieWebhook(hook);
        final ImageEmbed ie = new ImageEmbed();
        String nullCheck = "";
        if (header == null) {
            nullCheck = "Removed Header";
        }
        ie.setUrl(header);
        final DiscordEmbed de = DiscordEmbed.builder().author(AuthorEmbed.builder().name(account).icon_url(profilePic).url("https://twitter.com/" + account).build()).title(title).url(header).description(nullCheck).image(ie).footer(FooterEmbed.builder().text(this.footer).icon_url(this.footerIcon).build()).build();
        de.setColor(Integer.parseInt(this.color, 16));
        final DiscordMessage dm = DiscordMessage.builder().embeds(Arrays.asList(de)).build();
        temmie.sendMessage(dm);
    }
    
    public String replaceLinks(String message, final URLEntity[] links) {
        for (final URLEntity link : links) {
            message = message.replace(link.getURL(), link.getExpandedURL());
        }
        return message;
    }
    
    public String clickableTags(String desc) {
        List<String> allMatches = new ArrayList<String>();
        Matcher m = Pattern.compile("(?<=@)[\\w-]+").matcher(desc);
        while (m.find()) {
            allMatches.add(m.group());
        }
        for (final String match : allMatches) {
            desc = desc.replace("@" + match, "[@" + match + "](https://twitter.com/" + match + ")");
        }
        allMatches = new ArrayList<String>();
        m = Pattern.compile("#(\\w*[0-9a-zA-Z]+\\w*[0-9a-zA-Z])").matcher(desc);
        while (m.find()) {
            allMatches.add(m.group());
        }
        for (final String match : allMatches) {
            desc = desc.replace(match, "[" + match + "](https://twitter.com/hashtag/" + match.replace("#", "") + ")");
        }
        return desc;
    }
}