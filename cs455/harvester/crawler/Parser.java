package cs455.harvester.crawler;

/*
 *Author: Tiger Barras
 *Parser.java
 *Uses Jericho to parse webpages and pull links
 */

import net.htmlparser.jericho.*;

import java.io.IOException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.List;
import java.util.ArrayList;


public class Parser{

	//private final String baseUrl;
	private final String redirectedBaseUrl;
	private final Source source;


	public Parser(String url, Source _source){
		//this.baseUrl = url;
		this.redirectedBaseUrl = url;
		this.source = _source;

	}//End constructor

	//Retrieves all the a tags from the specified URL
	public List<Element> getTags() throws IOException{
		List<Element> aTags = null;

		//Webpage that needs to be parsed
		//Source source = new Source(new URL(baseUrl));

		//Get all 'a' tags
		aTags = this.source.getAllElements(HTMLElementName.A);

		return aTags;
	}//End getTags

	//Parses list of tags and returns a list of String URLs
	//Corrects for redirects
	public ArrayList<String> parseUrls(List<Element> aTags) throws IOException{
		String aTagUrl;
		ArrayList<String> urls = new ArrayList<String>();

		//First, see if the base is redirected
		//No longer have URLs because the parser now only operates on a source
		//this.redirectedBaseUrl = this.resolveRedirect(baseUrl);
		//System.out.println("RedirectedBaseUrl: " + this.redirectedBaseUrl);

		//Parse and clean urls
		for(Element aTag : aTags){
			//System.out.println("Grabbing href");
			aTagUrl = aTag.getAttributeValue("href");

			if(aTagUrl == null){
				System.out.println("null url on " + this.redirectedBaseUrl);
				continue;
			}

			//System.out.println("Making url absolute");
			aTagUrl = this.fixRelativeUrl(aTagUrl);

			//System.out.println("Resolving Redirect");
			// try{
			// 	aTagUrl = this.resolveRedirect(aTagUrl);
			// }catch(ClassCastException exception){
			// 	System.out.println("Parser: Swallowing ClassCastException");
			// }

			//Remove links to locations on a page
			// e.g. www.google.com/#bottom -> www.google.com/
			aTagUrl = cleanInterpageLink(aTagUrl);
			//Remove query strings
			aTagUrl = cleanQueryStrings(aTagUrl);

			//Only add the URL if it's in on of the OKed domains
			if(this.checkDomain(aTagUrl)){ urls.add(aTagUrl); }
		}

		return urls;
	}//End parseUrls

	//Checks to see if a url is in the list of OK domains
	private boolean checkDomain(String url){
		boolean flag = false;
		String prefix = "http://www.";
		String suffix = ".colostate.edu/";
		String[] domains = {"bmb", "biology", "chm", "cs", "math", "physics", "stat",
													"http://www.colostate.edu/Depts/Psychology"};

		//Generate full URLs for domains list, except psychology is already fine
		for(int i = 0; i < domains.length-1; i++){
			domains[i] = prefix + domains[i] + suffix;
		}

		//See if the arguement matches any of the domains
		for(String domain : domains){
			if(url.startsWith(domain)){ flag = true;}
		}

		return flag;
	}//End checkDoamin

	//Replaces URLs with their redirects
	//This is unnecessary because redirect checking is now handled in CrawlTask
	// public static String resolveRedirect(String url) throws IOException, ClassCastException {
	// 	//System.out.println("Resolving URL: " + url);
	//
  //   HttpURLConnection con = (HttpURLConnection)(new URL(url).openConnection());
  //   con.setInstanceFollowRedirects(false);
  //   con.connect();
  //   int responseCode = con.getResponseCode();
	// 	//System.out.println("response code: " + responseCode);
  //   if(responseCode == 301){
	// 			String location = con.getHeaderField( "Location" );
	// 			//System.out.println("Location: " + location);
  //       return location;
  //   } else {
  //       return url;
  //   }
	// }//End resolveRedirects

	//Prepends the base URL to any relative links
	private String fixRelativeUrl(String url){
		String absoluteUrlString = "";

		boolean absoluteUrlFlag = false;

		//Check for a null pointer here
		//This is a debug statement to figure out which URLs this happens to
		//Hopefully will never break here in final version
		try{
			absoluteUrlFlag = url.startsWith("http");
		}catch(NullPointerException exception){
			System.out.println("Parser: Null pointer exception when parsing "  + url);
			System.out.println(exception);
		}

		if(!absoluteUrlFlag){
			try{
				//These three lines create the absolute URL based on the baseURL
				URL baseURL = new URL(this.redirectedBaseUrl);
				URL absoluteUrl = new URL(baseURL, url);
				absoluteUrlString = absoluteUrl.toString();
			}catch(MalformedURLException exception){
				System.out.println("Parser: Error fixing relative url");
				System.out.println(exception);
			}
		}else{
			absoluteUrlString = url;
		}

		return absoluteUrlString;
	}//End fixRelativeUrl

	//Cleans off # links, because they just point to a location on a page
	private static String cleanInterpageLink(String url){
		if(url.lastIndexOf("#") == -1){
			return url;
		}else{
			//System.out.println("Cleaning link " + url);
			String newUrl = url.substring(0,url.lastIndexOf("#"));
			//System.out.println("new: " + newUrl);
			return newUrl;
		}
	}//End cleanInterpageLinks

	//Cleans off query Strings. Start with '?'
	private static String cleanQueryStrings(String url){
		if(url.lastIndexOf("?") == -1){
			return url;
		}else{
			//System.out.println("Cleaning link " + url);
			String newUrl = url.substring(0,url.lastIndexOf("?"));
			//System.out.println("new: " + newUrl);
			return newUrl;
		}
	}//End cleanInterpageLinks

	// //Takes the given base URL, follows any redirects, and then parses to just the path
	// private void fixBaseUrl() throws IOException{
	// 	this.redirectedBaseUrl = this.resolveRedirect(baseUrl);
	// 	try{
	// 		URL tempBase = new URL(redirectedBaseUrl);
	// 		this.redirectedBaseUrl = tempBase.getFile();
	// 	}catch(MalformedURLException exception){
	// 		System.out.println("Parser: Error resolving base address");
	// 		System.out.println(exception);
	// 	}
	// }//End fixBaseUrl

	//Returns a fully parsed set of URLs. No other method calls required
	public ArrayList<String> parseFully(){
		//Get all 'a' tags
		List<Element> aTags = null;
		try{
			aTags = this.getTags();
		}catch(IOException exception){
			System.out.println("Parser: Error reading a tags");
			System.out.println(exception);
		}

		//Get all the urls associated with those tags
		ArrayList<String> urlStrings = new ArrayList<String>();
		try{
			urlStrings = this.parseUrls(aTags);
		}catch(IOException exception){
			System.out.println("Parser: Error pulling URLs from a tag Strings");
			System.out.println(exception);
		}

		return urlStrings;
	}//End parseFully



	//This is the main from the example
	//Break it out into useful methods, and then implement in task
	public static void main(String args[]){
		//Disable verbose log statements
		Config.LoggerProvider = LoggerProvider.DISABLED;

		//Parser parser = new Parser("http://www.cs.colostate.edu/");

		// //Get all 'a' tags
		// List<Element> aTags = null;
		// try{
		// 	aTags = parser.getTags();
		// }catch(IOException exception){
		// 	System.out.println("Error");
		// 	System.out.println(exception);
		// }
		//
		// ArrayList<String> urlStrings = new ArrayList<String>();
		// try{
		// 	urlStrings = parser.parseUrls(aTags);
		// }catch(IOException exception){
		// 	System.out.println("Error 1");
		// 	System.out.println(exception);
		// }

		// ArrayList<String> urlStrings = parser.parseFully();
		//
		// System.out.println("Printing urls");
		// for(String url : urlStrings){
		// 	System.out.println(url);
		// }


	}//End main


}//End parser
