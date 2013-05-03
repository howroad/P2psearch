<%-- 
    Document   : index
    Created on : Mar 22, 2013, 3:14:08 PM    
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta charset="utf-8">
        <title>
            P2P Search
        </title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <meta name="description" content="p2p search Boston university college of computer engineering">
        <meta name="author" content="Paul, Pavas, Peiwen Hu">
        <!-- Le styles -->
        <link href="bootstrap.css" rel="stylesheet">
        <style>
            body { padding-top: 60px; }
        </style>
        <link href="bootstrap-responsive.css" rel="stylesheet">
        <style>
        </style>
    </head>
    <body>
        <div class="navbar navbar-inverse navbar-fixed-top">
            <div class="navbar-inner">
                <div class="container-fluid">
                    <a class="brand" href="#">P2P search</a>
                    <div class="nav-collapse collapse">
                        <p class="navbar-text pull-right">					
                            <a href="http://algorithmics.bu.edu/twiki/bin/view/EC504/FinalProject" class="navbar-link">EC 504 Final Project</a>
                        </p>
                        <ul class="nav">
                            <li class="active">
                                <a href="#">Home</a>
                            </li>
                            <li>
                                <a href="http://algorithmics.bu.edu/twiki/bin/view/EC504/GroupElevenSubmission">Report</a>
                            </li>
                        </ul>
                    </div>
                    <!--/.nav-collapse -->
                </div>
            </div>
        </div>
        <div class="container-fluid">
            <div class="row-fluid">
                <div class="span3">
                    <div class="well sidebar-nav">
                        <ul class="nav nav-list">
                            <li class="nav-header">
                                <form align="middle" name="Crawl Form" action="doCrawl">
                                    <h3 class="text-info">Crawl</h2>
                                        <hr>
                                        <h5>The starting website</h5>
                                        <div class="row-fluid span7 offset2">
                                            <span class="help-block">Please type in a valid http webpage address.</span>                                    
                                        </div>
                                        <input type="text" placeholder="www.bu.edu" name="seed" />

                                        <h5>the domain of the website</h5>
                                        <div class="row-fluid span7 offset2">
                                            <span class="help-block">Webpages crawled will only have this in their URLs,just for filtering.</span>
                                        </div>
                                        <input type="text" placeholder="bu.edu" name="domain" />

                                        <h4>Thread number</h4>
                                        <div class="row-fluid span7 offset2">
                                            <span class="help-block">How many threads do you want to use to crawl? 6 in maximum.</span>
                                        </div>
                                        <input type="text" placeholder="2" name="threadNum" />
                                        <br>
                                        <input class="btn btn-large btn-primary" type="submit" value="Crawl" />
                                </form>

                            </li>
                            <li>
                                <form  align="middle" name="Crawl Status Form" action="doCrawl">
                                    <input value="status" name="status" hidden="true"/>
                                    <input class="btn btn-info" type="submit" value="Check Status"  />
                                </form>
                            </li>
                        </ul>
                    </div>
                    <!--/.well -->
                </div>
                <!--/span-->
                <div class="span9">
                    <div class="hero-unit">
                        <h1 class="text-info" align ="middle">
                            Peer to Peer Search
                        </h1>
                    </div>
                    <form class="form-search" align="middle" name="Search Form" action="search">
                        <div class="input-append">
                            <input type="text" class="search-query" placeholder="EC504 solutions" name="keyword" />
                            <input  class="btn btn-primary" type="submit" value="search!" />
                        </div>
                    </form>

                </div>
                <!--/span-->
            </div>
            <!--/row-->
            <hr>
            <footer>
                <p>
                    © Group 11. Paul , Pavas &amp; Peiwen
                </p>
            </footer>
        </div>
        <!--/.fluid-container-->
    </body>
</html>
