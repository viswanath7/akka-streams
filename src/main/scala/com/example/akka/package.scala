package com.example

package object akka {

  case class Author(name: String)

  case class Hashtag(name: String){
    require(name.startsWith("#"), "Hash tag must start with a # symbol")
  }

  case class Tweet(author: Author, body: String) {
    def hashtags: Set[Hashtag] = {
      body.split(" ").collect{ case t if t.startsWith("#") => Hashtag(t)}.toSet
    }
  }

}
