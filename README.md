# Akka streams projects

This project provides two applications that utilise streams API of Akka:

 * SampleStreamingApplication that prints prime numbers till 1000 
 * TwitterTweets that print 100 tweets for a supplied hash tag.



## TwitterTweets

To run this spplication, do the following 

* Supply as a program argument, a hash tag that shall be used to search for tweets. Supplied hash tag must be prefixed with a #. **_#scala_** for instanse, is a valid hashtag.
* Go to [application management site of twitter](https://apps.twitter.com/) and login with your credentials. Create an application and go to the section `Application Settings > Consumer Key (API Key) > Manage keys and access tokens`
* Collect the values to authenticate your requests to the Twitter Platform. 
    - API Key, 
    - API secret, 
    - Access Token and 
    - Access token secret 
* Supply those values as the following environment variables 
    - API_KEY, 
    - API_SECRET, 
    - ACCESS_TOKEN & 
    - ACCESS_TOKEN_SECRET
* That's you've completed all the necessary preliminary work to run the application :+1: