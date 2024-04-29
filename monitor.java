import org.json.simple.*;
import java.io.*;
import org.json.simple.parser.*;
import twitter4j.*;
import java.util.*;

public class monitor implements Runnable
{
    private final ArrayList<String> accounts;
    private HashMap<String, String[]> hookEmbeds;
    private HashMap<String, ArrayList<String>> accountHooks;
    private int delay;
    private int attempts;
    private long listID;
    
    public monitor() throws IOException, ParseException {
        final JSONParser parser = new JSONParser();
        final FileReader fileReader = new FileReader("config.txt");
        final JSONObject config = (JSONObject)parser.parse(fileReader);
        this.delay = Integer.parseInt(config.get("delay"));
        this.listID = config.get("listID");
        this.attempts = 0;
        final FileReader embedFile = new FileReader("embed.txt");
        final JSONObject embed = (JSONObject)parser.parse(embedFile);
        final int webhooksAmt = embed.size();
        final String[][] embeds = new String[webhooksAmt][3];
        for (int x = 0; x < embed.size(); ++x) {
            final JSONArray embedArray = embed.get(new StringBuilder().append(x + 1).toString());
            embeds[x][0] = embedArray.get(0);
            embeds[x][1] = embedArray.get(1);
            embeds[x][2] = embedArray.get(2);
        }
        this.accounts = new ArrayList<String>();
        this.hookEmbeds = new HashMap<String, String[]>();
        this.accountHooks = new HashMap<String, ArrayList<String>>();
        for (int x = 0; x < webhooksAmt; ++x) {
            final Scanner scanAccounts = new Scanner(new File(String.valueOf(x + 1) + ".txt"));
            final String hook = scanAccounts.nextLine();
            this.hookEmbeds.put(hook, embeds[x]);
            while (scanAccounts.hasNextLine()) {
                final String account = scanAccounts.nextLine().toLowerCase();
                if (!this.accounts.contains(account)) {
                    this.accounts.add(account);
                    this.accountHooks.put(account, new ArrayList<String>());
                }
                this.accountHooks.get(account).add(hook);
            }
            scanAccounts.close();
        }
    }
    
    @Override
    public void run() {
        final Twitter twitter = TwitterFactory.getSingleton();
        ArrayList<String> oldBios = new ArrayList<String>();
        ArrayList<URLEntity> oldBioLinks = new ArrayList<URLEntity>();
        ArrayList<Boolean> oldPrivacies = new ArrayList<Boolean>();
        ArrayList<String> oldHeaders = new ArrayList<String>();
        PagableResponseList<User> users = null;
        try {
            users = twitter.getUserListMembers(this.listID, -1L);
            for (final User u : users) {
                oldBios.add(u.getDescription());
                oldBioLinks.add(u.getURLEntity());
                oldPrivacies.add(u.isProtected());
                oldHeaders.add(u.getProfileBannerURL());
            }
        }
        catch (TwitterException e) {
            e.printStackTrace();
        }
        while (true) {
            final ArrayList<String> bios = new ArrayList<String>();
            final ArrayList<URLEntity> bioLinks = new ArrayList<URLEntity>();
            final ArrayList<Boolean> privacies = new ArrayList<Boolean>();
            final ArrayList<String> headers = new ArrayList<String>();
            try {
                Thread.sleep(this.delay);
            }
            catch (InterruptedException ex) {}
            ++this.attempts;
            try {
                users = twitter.getUserListMembers(this.listID, -1L);
            }
            catch (TwitterException e2) {
                e2.printStackTrace();
            }
            for (final User u2 : users) {
                bios.add(u2.getDescription());
                bioLinks.add(u2.getURLEntity());
                privacies.add(u2.isProtected());
                headers.add(u2.getProfileBannerURL());
            }
            if (!oldBios.equals(bios)) {
                this.newBio(oldBios, bios, users);
                oldBios = bios;
            }
            if (!oldBioLinks.equals(bioLinks)) {
                this.newBioLink(oldBioLinks, bioLinks, users);
                oldBioLinks = bioLinks;
            }
            if (!oldPrivacies.equals(privacies)) {
                this.newPrivacy(oldPrivacies, privacies, users);
                oldPrivacies = privacies;
            }
            if (!oldHeaders.equals(headers)) {
                this.newHeader(oldHeaders, headers, users);
                oldHeaders = headers;
            }
            System.out.println("Attempt #" + this.attempts + " ... retrying list:" + this.listID + " (bio)");
        }
    }
    
    public void newBio(final ArrayList<String> oldBios, final ArrayList<String> bios, final PagableResponseList<User> users) {
        final ArrayList<String> newBios = new ArrayList<String>(bios);
        newBios.removeAll(oldBios);
        for (final String bio : newBios) {
            final int index = bios.indexOf(bio);
            final User u = users.get(index);
            System.out.println(bio);
            System.out.println("New bio - attempts reset (Previous attempts: " + this.attempts + ")");
            this.postDiscord(u.getScreenName(), bio, u.getBiggerProfileImageURL(), String.valueOf(u.getScreenName()) + " - Bio Change", u.getDescriptionURLEntities(), false);
        }
        this.attempts = 1;
    }
    
    public void newBioLink(final ArrayList<URLEntity> oldBios, final ArrayList<URLEntity> bios, final PagableResponseList<User> users) {
        final ArrayList<URLEntity> newBios = new ArrayList<URLEntity>(bios);
        newBios.removeAll(oldBios);
        for (final URLEntity bio : newBios) {
            final int index = bios.indexOf(bio);
            final User u = users.get(index);
            final String longURL = bio.getExpandedURL();
            System.out.println(longURL);
            System.out.println("New bio link - attempts reset (Previous attempts: " + this.attempts + ")");
            this.postDiscord(u.getScreenName(), longURL, u.getBiggerProfileImageURL(), String.valueOf(u.getScreenName()) + " - Bio Link Change", null, false);
        }
        this.attempts = 1;
    }
    
    public void newPrivacy(final ArrayList<Boolean> oldPrivacies, final ArrayList<Boolean> privacies, final PagableResponseList<User> users) {
        for (int x = 0; x < privacies.size(); ++x) {
            if (privacies.get(x) != oldPrivacies.get(x)) {
                final User u = users.get(x);
                System.out.println("New privacy - attempts reset (Previous attempts: " + this.attempts + ")");
                String desc = u.getScreenName();
                if (privacies.get(x)) {
                    desc = String.valueOf(desc) + " is now private. Be ready!";
                }
                else {
                    desc = String.valueOf(desc) + " is no longer private.";
                }
                this.postDiscord(u.getScreenName(), desc, u.getBiggerProfileImageURL(), "Profile Status", null, false);
            }
        }
        this.attempts = 1;
    }
    
    public void newHeader(final ArrayList<String> oldHeaders, final ArrayList<String> headers, final PagableResponseList<User> users) {
        final ArrayList<String> newHeaders = new ArrayList<String>(headers);
        newHeaders.removeAll(oldHeaders);
        for (final String header : newHeaders) {
            final int index = headers.indexOf(header);
            final User u = users.get(index);
            System.out.println(header);
            System.out.println("New header - attempts reset (Previous attempts: " + this.attempts + ")");
            this.postDiscord(u.getScreenName(), header, u.getBiggerProfileImageURL(), "Header Change", null, true);
        }
        this.attempts = 1;
    }
    
    public void postDiscord(final String account, final String desc, final String profilePic, final String title, final URLEntity[] links, final boolean header) {
        final ArrayList<String> hooks = this.accountHooks.get(account.toLowerCase());
        if (hooks != null) {
            for (final String hook : hooks) {
                final String[] embeds = this.hookEmbeds.get(hook);
                final Discord d = new Discord(embeds[0], embeds[1], embeds[2]);
                if (!header) {
                    d.shortHook(hook, desc, profilePic, title, links);
                }
                else {
                    d.header(hook, desc, profilePic, title, account);
                }
            }
        }
    }
}
