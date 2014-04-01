package newsrack.web.api;

import com.opensymphony.xwork2.Action;
import newsrack.archiver.Source;
import newsrack.database.NewsItem;
import newsrack.filter.Category;
import newsrack.filter.Issue;
import newsrack.user.User;
import org.apache.struts2.interceptor.ServletRequestAware;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * class <code>NewsAction</code> implements the functionality of fetching news and other news-related info
 */
public class NewsAction extends BaseApiAction implements ServletRequestAware
{
   private static final ThreadLocal<SimpleDateFormat> DATE_PARSER = new ThreadLocal<SimpleDateFormat>() {
		protected SimpleDateFormat initialValue() { return new SimpleDateFormat("yyyy.MM.dd"); }
	};

	private HttpServletRequest _req;
	public void setServletRequest(HttpServletRequest request) { _req = request; }

	private List<NewsItem> _news;
	public List<NewsItem> getNewsList() { return _news; }

	private Category _cat;
	public Category getCategory() { return _cat; }

	public String getSiteUrl() { return newsrack.NewsRack.getServerURL(); }

	/**
	 * GET /api/news?<PARAMS>
	 *
	 * . owner=UID
	 * . issue=ISSUE
	 * . catID=ID
	 * . source_tag=SOURCE   (optional)
	 * . start_date=START    (optional)
	 * . end_date=END        (optional)
	 * . start=N             (optional)
	 * . count=N (max = 100) (optional)
	 **/
   public String getNews()
	{
		try {
				// User - mandatory
			String uid = getApiParamValue("owner", false);
			User   u   = (uid == null) ? null: User.getUser(uid);
			if (!validateParam(u, "owner", uid))
				return Action.ERROR;

				// Issue - mandatory
			String issueName = getApiParamValue("issue", false);
			Issue  i         = (issueName == null) ? null : u.getIssue(issueName);
			if (!validateParam(i, "issue", issueName))
				return Action.ERROR;

				// Cat Id - optional
			Category c = null;
			String catId = getApiParamValue("catID", true);
			if (catId != null) {
				c = i.getCategory(Integer.parseInt(catId));
				if (!validateParam(c, "catID", catId))
					return Action.ERROR;
			}

				// start - optional
			int start = -1;
			String startStr = getApiParamValue("start", true);
			if (startStr != null)
				start = Integer.parseInt(startStr);
			if (start < 0)
				start = 0;

				// count - optional
			int count = -1;
			String countStr = getApiParamValue("count", true);
			if (countStr != null)
				count = Integer.parseInt(countStr);
			if (count > 100)
				count = 100;
			if (count < 0)
				count = 20;

				// start date - optional
			Date startDate = null;
			String sdStr = getApiParamValue("start_date", true);
			if (sdStr != null) {
				try {
					startDate = DATE_PARSER.get().parse(sdStr);
				}
				catch (Exception e) {
					_errMsg = getText("api.bad.date", sdStr);
					_log.error("Error parsing date: " + sdStr, e);
					return Action.ERROR;
				}
			}

				// end date - optional
			Date endDate = null;
			String edStr = getApiParamValue("end_date", true);
			if (edStr != null) {
				try {
					endDate = DATE_PARSER.get().parse(edStr);
				}
				catch (Exception e) {
					_errMsg = getText("api.bad.date", edStr);
					_log.error("Error parsing date: " + edStr, e);
					return Action.ERROR;
				}
			}

				// source tag - optional
			String srcKey = getApiParamValue("source_key", true);
			Source src    = null;
			if (srcKey != null)
		      try { src = i.getUser().getSourceByKey(Long.parseLong(srcKey)); } catch (Exception e) { }

			_log.info("API: owner uid - " + uid + "; issue name - " + issueName + "; catID - " + catId + "; start - " + start + "; count - " + count + "; start - " + startDate + "; end - " + endDate + "; srcKey - " + srcKey + "; src - " + (src != null ? src.getKey() : null));

				// Set up news
			_news = (c == null) ? i.getNews(startDate, endDate, src, start, count)
			                    : c.getNews(startDate, endDate, src, start, count);

			_cat = c;

			return apiSuccess();
		}
		catch (Exception e) {
			_log.error("API: News: Error fetching news!", e);
			_errMsg = getText("internal.app.error");
			return Action.ERROR;
		}
	}

	/**
	 * GET /api/newsInfo?<PARAMS>
	 *
	 * . url=URL (url-encoded, of course)
	 **/
	private NewsItem _newsItem;
	public NewsItem getNewsItem() { return _newsItem; }

	private List<Category> _cats;
	public List<Category> getCategories() { return _cats; }

	public String getNewsInfo()
	{
		String uidList = getApiParamValue("uids", true); 	// User id list, comma-separated - optional
		String uid     = getApiParamValue("uid", true); 	// User - optional
		String tname   = getApiParamValue("topic", true);  // Topic Name - optional (only works in conjunction with uid)
		if (tname == null)
			tname = getApiParamValue("issue", true); // Accept issue too - optional (only works in conjunction with uid)

			// The url param is always the last parameter -- get it from the query string.  Using getApiParamValue will not work
			// because if the target url has url params itself (&x=v), those will get stripped from the target url by the interceptor
			// that sets these params (obviously, since it doesn't know anything about where the url begins and ends!)
		String url = _req.getQueryString().replaceAll(".*url=", "");

			// Reduce url to canonical form before querying!
		newsrack.util.URLCanonicalizer.canonicalize(url);
		_newsItem = NewsItem.getNewsItemFromURL(url);
		if (_newsItem == null) {
			_errMsg = getText("url.not.found");
			return Action.ERROR;
		}

			// Build a list of all leaf and non-leaf categories for this news item
		List<Category> allCats = _newsItem.getAllCategories();
		_cats = new ArrayList<Category>();
			// No filtering by uids
		if ((uid == null) && (uidList == null)) {
			_cats = allCats;
		}
			// Filtering by a set of uids
		else if (uidList != null) {
				// split at ',' and remove white space
			String[] uids = uidList.split(",");
			for (int i = 0; i < uids.length; i++)
				uids[i] = uids[i].trim();

			for (Category c: allCats) {
				String catUid = c.getUser().getUid();
				for (String u: uids) {
					if (u.equals(catUid)) {
						_cats.add(c);
						break;
					}
				}
			}
		}
			// Filter by uid/issue
		else {
			for (Category c: allCats) {
				if (uid.equals(c.getUser().getUid()) && ((tname == null) || tname.equals(c.getIssue().getName())))
					_cats.add(c);
			}
		}

		return apiSuccess();
	}
}
