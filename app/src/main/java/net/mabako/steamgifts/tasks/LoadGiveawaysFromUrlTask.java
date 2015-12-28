package net.mabako.steamgifts.tasks;

import android.os.AsyncTask;
import android.util.Log;

import net.mabako.steamgifts.fragments.GiveawaysFragment;
import net.mabako.steamgifts.data.Giveaway;
import net.mabako.steamgifts.web.WebUserData;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class LoadGiveawaysFromUrlTask extends AsyncTask<Void, Void, List<Giveaway>> {
    private static final String TAG = LoadGiveawaysFromUrlTask.class.getSimpleName();

    private GiveawaysFragment activity;
    private int page;
    private GiveawaysFragment.Type type;

    public LoadGiveawaysFromUrlTask(GiveawaysFragment activity, int page, GiveawaysFragment.Type type) {
        this.activity = activity;
        this.page = page;
        this.type = type;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected List<Giveaway> doInBackground(Void... params) {
        Log.d(TAG, "Fetching giveaways for page " + page);

        try {
            // Fetch the Giveaway page
            String typeStr = type == GiveawaysFragment.Type.ALL ? "" : ("&type=" + type.name().toLowerCase());

            Connection jsoup = Jsoup.connect("http://www.steamgifts.com/giveaways/search?page=" + page + typeStr);
            if (WebUserData.getCurrent().isLoggedIn())
                jsoup = jsoup.cookie("PHPSESSID", WebUserData.getCurrent().getSessionId());
            Document document = jsoup.get();

            WebUserData.extract(document);

            // Parse all rows of giveaways
            Elements giveaways = document.select(".giveaway__row-inner-wrap");
            Log.d(TAG, "Found inner " + giveaways.size() + " elements");

            List<Giveaway> giveawayList = new ArrayList<>();
            for(Element element : giveaways)
            {
                Element link = element.select("h2 a").first();
                Element icon = element.select("h2 a").last();

                // Base information
                String title = link.text();
                String giveawayLink = link.attr("href").substring(10, 15);
                int gameId = Integer.parseInt(icon.attr("href").split("/")[4]);

                // Entries & Comments
                Elements links = element.select(".giveaway__links a span");
                int entries = Integer.parseInt(links.first().text().split(" ")[0].replace(",", ""));
                int comments = Integer.parseInt(links.last().text().split(" ")[0].replace(",", ""));

                String creator = element.select(".giveaway__username").text();

                // Copies & Points. They do not have separate markup classes, it's basically "if one thin markup element exists, it's one copy only"
                Elements hints = element.select(".giveaway__heading__thin");
                String copiesT = hints.first().text();
                String pointsT = hints.last().text();
                int copies = hints.size() == 1 ? 1 : Integer.parseInt(copiesT.replace("(", "").replace(" Copies)", ""));
                int points = Integer.parseInt(pointsT.replace("(", "").replace("P)", ""));

                Log.v(TAG, "GIVEAWAY for " + title + ", " + giveawayLink + ", " + gameId);

                giveawayList.add(new Giveaway(title, giveawayLink, gameId, creator, entries, comments, copies, points));
            }

            return giveawayList;
        } catch (Exception e) {
            Log.e(TAG, "Error fetching URL", e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(List<Giveaway> result) {
        super.onPostExecute(result);
        activity.addGiveaways(result, page == 1);
    }
}
