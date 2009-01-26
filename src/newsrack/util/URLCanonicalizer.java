package newsrack.util;

import newsrack.NewsRack;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;
import java.util.regex.Pattern;

import newsrack.util.StringUtils;

import org.htmlparser.http.ConnectionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.opensymphony.oscache.base.*;
import com.opensymphony.oscache.extra.*;
import com.opensymphony.oscache.general.*;

// This class canonicalizes urls of news stories so that we can more
// easily recognize identical urls.  For now, this class hardcodes rules
// for a few sites.  In future, this information should come from
// an external file that specifies match-rewrite rules for urls
public class URLCanonicalizer
{
   static Log _log = LogFactory.getLog(URLCanonicalizer.class);

	static ConnectionManager cm;

		/* These are patterns for detecting feed proxies */
	static String[] proxyREStrs = new String[] { 
		"^feeds\\..*$", "^feedproxy\\..*$", "^.*\\.feedburner.com$", "^pheedo.com$"
	};

		/* Domains & corresponding url-split rule */
	static String[] urlFixupRuleStrings = new String[] {
      "sfgate.com:&feed=.*", "marketwatch.com:&dist=.*", "bloomberg.com:&refer=.*", "cbsnews.com:\\?source=[^?&]*",
		"vaildaily.com:\\/-1\\/rss.*", "news.newamericamedia.org:&from=.*"
   };

   	/* Domains for which we'll replace all ?.* url-tracking parameters */
	static String[] domainsWithDefaultFixupRule = new String[] {
		"nytimes.com", "rockymountainnews.com",
		"newscientist.com", "washingtonpost.com", "guardian.co.uk",
		"boston.com", "publicradio.org", "cnn.com", "chicagotribune.com",
		"latimes.com", "twincities.com", "mercurynews.com", "wsj.com",
		"seattletimes.nwsource.com", "reuters.com", "sltrib.com",
		"nation.com", "salon.com", "newsweek.com", "forbes.com"
   };

	static Pattern[] proxyREs;
	static HashMap<String,Pattern> urlFixupRules;
	static GeneralCacheAdministrator _urlCache;

	static {
			// Set up some default connection properties!
		Hashtable headers = new Hashtable();
		String ua = NewsRack.getProperty("useragent.string");
		if (ua == null) ua = "NewsRack/1.0 (http://newsrack.in)";
		headers.put ("User-Agent", ua);
      headers.put ("Accept-Encoding", "gzip, deflate");

			// Build a url cache
		Properties p = new Properties();
		p.setProperty("cache.memory", "true");
		p.setProperty("cache.event.listeners", "com.opensymphony.oscache.extra.CacheEntryEventListenerImpl, com.opensymphony.oscache.extra.CacheMapAccessEventListenerImpl");
		String capacity = NewsRack.getProperty("urls.cache.size");
		if (capacity == null) capacity = "10000";
		p.setProperty("cache.capacity", capacity);
		// --> Don't use disk cache for now
		// p.setProperty("cache.persistence.class", "com.opensymphony.oscache.plugins.diskpersistence.HashDiskPersistenceListener");
		// p.setProperty("cache.path", NewsRack.getProperty("cache.path"));
		_urlCache = new GeneralCacheAdministrator(p);

			// Turn off automatic redirect processing
		java.net.HttpURLConnection.setFollowRedirects(false);

			// Set up a connection manager to follow redirects while using cookies
		cm = new ConnectionManager();
		cm.setRedirectionProcessingEnabled(true);
		cm.setCookieProcessingEnabled(true);
		cm.setDefaultRequestProperties(headers);

			// Compile proxy domain patterns 
		proxyREs = new Pattern[proxyREStrs.length];
		int i = 0;
		for (String re: proxyREStrs) {
			proxyREs[i] = Pattern.compile(re);
			i++;
		}

			// Custom url fixup rules
		urlFixupRules = new HashMap<String, Pattern>();
		for (String s: urlFixupRuleStrings) {
			String[] x = s.split(":");
			urlFixupRules.put(x[0], Pattern.compile(x[1], Pattern.CASE_INSENSITIVE));
		}
	}

	private static boolean isFeedProxyUrl(String url)
	{
		String d = StringUtils.getDomainForUrl(url);
		if (_log.isDebugEnabled()) _log.debug("Domain: " + d);
		for (Pattern p: proxyREs) {
			if (p.matcher(d).matches()) {
				if (_log.isDebugEnabled()) _log.debug("PATTERN " + p.pattern() + " succeeded");
				return true;
			}
			else {
				if (_log.isDebugEnabled()) _log.debug("PATTERN " + p.pattern() + " failed");
			}
		}

		return false;
	}

	public static String cleanup(String baseUrl, String url)
	{
			// get rid of all white space!
		url = url.replaceAll("\\s+", "");
			// Is this allowed in the spec??? Some feeds (like timesnow) uses relative URLs!
		if (url.startsWith("/"))
			url = baseUrl + url;

		return url;
	}

	private static String getTargetUrl(String url)
	{
		String targetUrl = null;
		try {
				// Check if we have resolved this url already, and if so, get it from the cache!
			targetUrl = (String)_urlCache.getFromCache(url);
			_log.info("Returning " + targetUrl + " from cache for " + url);
			return targetUrl;
		}
		catch (NeedsRefreshException nre) {
				// Doesn't exist ... go fetch!
			java.net.URLConnection conn = null;
			try {
				conn = cm.openConnection(new java.net.URL(url));
				targetUrl = conn.getURL().toString();
			}
			catch (Exception e) {
				_log.error("Error getting canonicalized url for : " + url + "; Exception: " + e);
				String msg = e.toString();
				int    i   = msg.indexOf("no protocol:");
				if (i > 0 && url != null) {
					String domain    = url.substring(0, url.indexOf("/", 7));
					String urlSuffix = msg.substring(i + 13);
					String newUrl    = domain + urlSuffix;
					_log.info("Got malformed url exception " + msg + "; Retrying with url - " + newUrl);
					targetUrl = getTargetUrl(newUrl);
				}
				else {
					if (_log.isDebugEnabled()) _log.debug("Got exception: " + e);
					targetUrl = url;
				}
			}
			finally {
				if (conn != null) {
					if (conn instanceof java.net.HttpURLConnection)
						((java.net.HttpURLConnection)conn).disconnect();
					else
						_log.error("Connection is not a HttpURLConnection!");
				}

					// Record in cache / cancel the update depending on whether we successfully resolved the proxy url!
				if (targetUrl != null) {
					if (targetUrl != url)
						_urlCache.putInCache(url, targetUrl);
					else
						_urlCache.cancelUpdate(url);
				}

				return targetUrl;
			}
		}
	}

	public static String canonicalize(String url)
	{
		boolean repeat;
		do {
		   repeat = false;

				// Special rule for news.google.com
			if (url.indexOf("news.google.com/") != -1) {
				int proxyUrlStart = url.lastIndexOf("http://");
				if (proxyUrlStart != -1) {
					url = url.substring(proxyUrlStart);
					url = url.substring(0, url.lastIndexOf("&cid="));
				}
			}

				// Follow proxies
			if (isFeedProxyUrl(url))
				url = getTargetUrl(url);

			String domain = StringUtils.getDomainForUrl(url);
			if (_log.isDebugEnabled()) _log.debug("Domain for default rule: " + domain);

			Pattern p = urlFixupRules.get(domain);
			if (p != null) {
					// Default fixup rule
				if (_log.isDebugEnabled()) _log.debug("Found a pattern: " + p.pattern());
				String newUrl = p.split(url)[0];
				if (!newUrl.equals(url))
					url = newUrl;
			}
			else {
					// Default fixup rule
				for (String d: domainsWithDefaultFixupRule) {
					if (domain.indexOf(d) != -1) {
						int i = url.indexOf("?");
						if (i > 0) {
							url = url.substring(0, i);
						}
						break;
					}
				}
			}

				// Get rid of redirects
			if (!url.startsWith("http://uni.medhas.org")) {
				int i = url.lastIndexOf("http://");
				if (i == -1)
				  i = url.lastIndexOf("https://");
				if (i != -1)
					url = url.substring(i);
			}
		} while(repeat);

		return url;
	}

	public static void main(String[] args)
	{
		System.out.println("input - " + args[0] + "; output - " + canonicalize(args[0]));
	}
}
