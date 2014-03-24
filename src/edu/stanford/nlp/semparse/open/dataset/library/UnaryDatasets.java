package edu.stanford.nlp.semparse.open.dataset.library;

import edu.stanford.nlp.semparse.open.dataset.Dataset;

class UnaryDatasets {

  public Dataset getDataset(final String name) {
    if (name == null)
      throw new RuntimeException("No dataset specified.");
    
    if (name.equals("all")) {
      return new Dataset()
          .addTestFromDataset(getDataset("geo"))
          .addFromDataset(getDataset("academia"))
          .addFromDataset(getDataset("website"))
          .addFromDataset(getDataset("stanford"))
          .addFromDataset(getDataset("route"))
          .addFromDataset(getDataset("celeb"))
          .addFromDataset(getDataset("sport"))
          .addFromDataset(getDataset("leader"))
          .addFromDataset(getDataset("fiction"));
    }
    
    return new Dataset() {
      {
        switch (name) {
          case "one":
            E("European countries", L("Greece", "Germany", "Spain", "France", "Estonia", "Romania"));
            break;
          
          case "geo":
            // Easy examples: every page has roughly, should be easy to generalize
            E("European countries", L("Greece", "Germany", "Spain", "France", "Estonia", "Romania"));
            E("Asian countries", L("Japan", "China", "India", "Singapore", "Kyrgyzstan", "Iran"));
            E("Canada provinces", L("Quebec", "British Columbia", "Ontario", "Saskatchewan"));
            E("cities in California", L("Los Angeles", "San Jose", "Ontario", "Sacramento", "San Francisco"));
            E("Hawaii islands", L("Hawaii", "Maui", "Kauai", "Molokai", "Oahu", "Lanai", "Niihua"));
            E("states of the USA", L("California", "Ohio", "Alaska", "Michigan", "Kansas", "New Jersey", "Arizona"));
            break;
            
          case "academia":
            E("stanford cs faculty", LN("Percy Liang", "Andrew Ng", "Alex Aiken", "Don Knuth", "Chris Manning"));
            E("cmu cs faculty", LN("Avrim Blum", "Umut Acar", "Priya Narasimhan", "Mahadev Satyanarayanan"));
            E("Michael I Jordan students", LN("Percy Liang", "Tommi Jaakkola", "John Duchi"));
            E("Lillian Lee students", LN("Regina Barzilay", "Chenhao Tan", "Bo Pang", "Rie Johnson"));
            E("MIT CSAIL professors", LN("Daniel Jackson", "Eric Grimson", "Hal Abelson", "Shafi Goldwasser"));
            break;
            
          case "website":
            E("online social networks", L("Facebook", "Twitter", "Myspace", "Google+"));
            E("search engines", L("Google", "Yahoo", "Bing"));
            E("Chinese web portals", L("Baidu", "Sina", "Sohu"));
            E("social bookmarking sites", L("Reddit", "StumbleUpon", "Digg", "Delicious"));
            break;
            
          case "stanford":
            // Web pages are a little harder to parse,
            // but may be closer to what general people want to know
            E("Stanford undergraduate residence halls", LN("Branner Hall", "Lagunita Court", "Wilbur Hall"));
            E("Stanford departments", L("Anesthesia", "Dermatology", "Linguistics", "Geophysics"));
            E("stores in Stanford Shopping Center", L("Brookstone", "Gap", "Microsoft", "Urban Outfitters"));
            E("Stanford Marguerite lines", L("Line X", "Line O", "SLAC", "Shopping Express", "Bohannon"));
            E("dining halls in Stanford", L("Ricker", "Wilbur", "Branner", "Lakeside"));
            E("libraries in Stanford", L("Green", "Meyer", "Hoover", "East Asia", "Music"));
            break;
            
          case "route":
            // The gas station question is tricky: what should the answer format be?
            E("Caltrain stops", L("San Francisco", "Palo Alto", "Mountain View", "Santa Clara", "Millbrae"));
            E("Boston red line stations", L("Harvard Square", "Kendall", "Broadway", "South Station", "Braintree"));
            E("Tokyo Metro subway lines", L("Ginza", "Chiyoda", "Hibiya", "Namboku", "Fukutoshin", "Marunouchi"));
            break;
            
          case "celeb":
            E("Justin Bieber's albums", L("Believe", "Under the Mistletoe", "Never Say Never", "My World 2.0"));
            E("Shyamalan's movies", L("The Sixth Sense", "Unbrekable", "After Earth", "Signs"));
            E("Rebecca Black singles", L("Friday", "My Moment", "Person of Interest", "Sing It", "In Your Words"));
            E("members of The Beetles", LN("John Lennon", "Paul McCartney", "George Harrison", "Ringo Starr"));
            E("casts of The Room", LN("Tommy Wiseau", "Greg Sestero", "Juliette Danielle", "Philip Haldiman"));
            break;
            
          case "sport":
            E("world cup champions", L(true, "Brazil", "Spain", "Argentina", "Uruguay", "Italy", "France", "England", "Germany"));
            E("England football clubs", L("Manchester United", "Liverpool", "Chelsea", "Arsenal", "Manchester City"));
            E("football teams in California", L("Raiders", "Chargers", "49ers"));
            E("countries in olympics 2012", L("China", "United States", "Australia", "Azerbaijan", "North Korea"));
            E("Wimbledon winners in men single", LN("Andy Murray", "Roger Federer", "Rafael Nadal", "Lleyton Hewitt"));
            break;
            
          case "leader":
            E("world billionaires", LN("Bill Gates", "Warren Buffett", "Larry Page", "Larry Ellison", "Steve Ballmer"));
            E("united states presidents", LN("George Washington", "Thomas Jefferson", "Abraham Lincoln",
                "Richard Nixon", "Barack Obama", "Andrew Jackson", "Bill Clinton"));
            E("united states vice presidents", LN("Joe Biden", "Al Gore", "Nelson Rockefeller", "Dick Cheney", "Aaron Burr"));
            E("leaders of ussr", LN("Vladimir Lenin", "Joseph Stalin", "Nikita Khrushchev", "Leonid Brezhnev", "Mikhail Gorbachev"));
            E("provosts of Stanford University", LN("Douglas M. Whitaker", "Gerald J. Lieberman",
                "Donald Kennedy", "John Etchemendy", "Richard Wall Lyman", "Condoleezza Rice"));
            break;
            
          case "fiction":
            E("Hogwarts Houses", L(true, "Gryffindor", "Hufflepuff", "Ravenclaw", "Slytherin"));
            E("main characters of Friends", LN(true, "Rachel Green", "Monica Geller", "Phoebe Buffay",
                "Joey Tribbiani", "Chandler Bing", "Ross Geller"));
            E("Twilight Saga books", L(true, "Twilight", "New Moon", "Eclipse", "Breaking Dawn"));
            E("disney movies", L("Brave", "Alice In Wonderland", "Wall E", "The Jungle Book", "Pinocchio"));
            break;
            
          default:
            throw new RuntimeException("Unsupported dataset: " + name);
        }
      }
    };
  }

}
