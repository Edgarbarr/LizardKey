package org.lizard;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Actions {
    private final Board board;
    private final Player player;

    public Actions(Board board, Player player) {
        this.board = board;
        this.player = player;
    }

    public void execute(Command command) {
        GameDictionary.Noun noun;
        GameDictionary.Noun targetNoun;
        Integer verb;
        if(command.isAmbiguous()) {
            disambiguate(command);
            return;
        }

        verb = command.getVerb();
        noun = command.getNoun() == null ? null : command.getNoun()[0];
        targetNoun = null;
        if(command.getTargetNoun() != null) {
            targetNoun = command.getTargetNoun()[0];

        }


        if (verb == null && noun == null){
            System.out.println("Wrong command");
            return;
        }
        if(verb == null) {
            System.out.println(noun.getDescription());
            return;
        }
        if(targetNoun != null && verb != 4) {
            System.out.println("You tried to use a " + verb + " with 2 nouns? What you trying to break my code or something?");
        }
        else{
            switch(verb) {
                case 1:
                    grab(noun);
                    break;
                case 2:
                    move(noun);
                    break;
                case 3:
                    examine(noun);
                    break;
                case 4:
                    use(noun, targetNoun);
                    break;
                case 5:
                    drop(noun);
                    break;
            }
        }
    }

    private void move(GameDictionary.Noun direction) {

        if(direction instanceof Directions.Direction) {
            board.changeCurrentRoom(((Directions.Direction) direction).getDirection());
        } else {
            System.out.println("What??? you can't go there.");
        }

    }


    private void grab(GameDictionary.Noun noun) {

        if(noun == null) {
            System.out.println("That doesn't exist");
        }
        else if(!noun.isGrabable()) {
            System.out.println("You can't even grab a " + noun.getName());
        } else if(player.getInventory().has(noun)){
            System.out.println("You already have that in your inventory");
        } else {
            //get current room
            Room currentRoom = board.getCurrentRoom();
            //check if the room has the item else return fail
            GameDictionary.Noun grabbedItem = currentRoom.grabItem((Item) noun);
            //if it does exist pop it off room item list
            if(grabbedItem != null) {
                player.getInventory().add(grabbedItem);
                System.out.println("You grabbed the " + noun.getName());
            } else {
                System.out.println("You can't");
            }

        }
    }

    public void use(GameDictionary.Noun noun, GameDictionary.Noun targetNoun) {
        if(targetNoun instanceof Directions.Direction) {
            unlockExit(((Directions.Direction) targetNoun).getDirection(), noun);
            return;
        }
        if(targetNoun == null) {
            System.out.println("Use it on what?");
            return;
        }
        Lock targetLock = targetNoun.getLock();

        if(targetLock == null) {
            System.out.println("Thats not how this works.");
            return;
        }
        if(!player.getInventory().has((Item) noun)) {
            System.out.println("You don't have a " + noun.getName() + " in your inventory.");
            return;
        }
        if(!board.getCurrentRoom().has((Item) targetNoun) && !player.getInventory().has((Item) targetNoun)) {
            System.out.println("There isn't a " + targetNoun.getName() + " here.");
            return;
        }
        if(targetLock.getNoun() == noun) {
            targetLock.printDescription();
            this.execute(targetLock.getCommand());
            targetNoun.deleteLock();

        }
    }
    public void unlockExit(String direction, GameDictionary.Noun noun) {
        if(!player.getInventory().has(noun)) {
            System.out.println("You don't have that on your person");
            return;
        }
        Lock lock = board.getCurrentRoom().getLock(direction);
        if(lock.getNoun().equals(noun)) {
            lock.printDescription();
            board.getCurrentRoom().removeLock(direction);
            this.execute(lock.getCommand());
        } else {
            System.out.println("What did you think that would even accomplish?");
        }
    }

    private void examine(GameDictionary.Noun noun){
       Room currentRoom = board.getCurrentRoom();

        if(noun == null){
            System.out.println("Examining room...");
            //prints description of the current room
            System.out.println(currentRoom.getRoomDescription());
            return;
        }
        if(noun.getName().equals("inventory")) {
            System.out.println(player.getInventory().getDescription());
            return;
        }
        if(!player.getInventory().has(noun) && !currentRoom.has(noun)) {
            System.out.println("You can examine what's not there");
        } else{
            System.out.println(noun.getDescription());
        }



    }

    public void drop(GameDictionary.Noun noun) {
        GameDictionary.Noun droppedItem = player.getInventory().drop(noun);
        if(droppedItem == null) {
            System.out.println(player.getName() + " how are you gonna drop something you don't have? How?");
        } else {
            board.getCurrentRoom().addItemToRoom(droppedItem);
            System.out.println("You dropped the " + droppedItem.getName() + " in the " + board.getCurrentRoom().getName());
        }

    }

    private void disambiguate(Command command) {
        GameDictionary.Noun[] noun = command.getNoun();
        GameDictionary.Noun[] targetNoun = command.getTargetNoun();

        Set<GameDictionary.Noun> availableNouns = new HashSet<>(player.getInventory().getItems());
        availableNouns.addAll(board.getCurrentRoom().getItems());

        Set<GameDictionary.Noun> nounSet = new HashSet<>(Arrays.asList(noun));

        nounSet.retainAll(availableNouns);

        if(nounSet.size() == 1) {
            noun = new GameDictionary.Noun[]{nounSet.iterator().next()};
        } else if(nounSet.size() > 1) {
            Iterator<GameDictionary.Noun> iterator = nounSet.iterator();
            iterator.next();
            noun = specifyNoun(nounSet);
        } else {
            noun = null;
        }

        if(targetNoun != null) {
            Set<GameDictionary.Noun> targetNounSet = new HashSet<>(Arrays.asList(targetNoun));
            targetNounSet.retainAll(availableNouns);

            if(targetNounSet.size() == 1) {
                targetNoun = new GameDictionary.Noun[]{targetNounSet.iterator().next()};
            } else if(targetNounSet.size() > 1) {
                Iterator<GameDictionary.Noun> iterator = targetNounSet.iterator();
                iterator.next();
                noun = specifyNoun(targetNounSet);
            }

        }

        if(targetNoun != null && targetNoun.length == 1) {
            execute(new Command(command.getVerb(), noun, targetNoun));
        } else {
            execute(new Command(command.getVerb(), noun));
        }

    }
    public GameDictionary.Noun[] specifyNoun(Set<GameDictionary.Noun> nounSet) {
        List<GameDictionary.Noun> nounList = new ArrayList<>(nounSet);

        while(true) {
            nounList.forEach(noun -> {
                System.out.println("You see a "+ noun.getName());
            });
            String userInput = Game.prompter.promptPlayer("Which one?");
            List<GameDictionary.Noun> validNoun = new ArrayList<>();
            int i = 0;
            for(int j = 0; j < nounList.size(); j++) {
                if(nounList.get(j).getName().contains(userInput)) {
                    validNoun.add(nounList.get(j));
                }
            }

            if(validNoun.size() == 1) {
                return validNoun.toArray(new GameDictionary.Noun[1]);
            } else {
                System.out.println("Can you read? Pick one.");
            }

        }
    }
}
