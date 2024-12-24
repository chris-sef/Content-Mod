import java.util.*;

// Base class for accounts
abstract class Account {
    String username;
    String password;

    public Account(String username, String password) {
        this.username = username;
        this.password = password;
    }
}

// User class
class User extends Account {
    List<String> posts = new ArrayList<>();
    Map<String, Boolean> flaggedAppeals = new HashMap<>();

    public User(String username, String password) {
        super(username, password);
    }

    public void createPost(String postId, String content, ContentModerationSystem cms) {
        cms.addPost(postId, content, this);
        posts.add(postId);
    }

    public void viewOwnPosts(ContentModerationSystem cms) {
        for (String postId : posts) {
            Post post = cms.getPost(postId);
            System.out.println(post);
            System.out.println("---------------");
        }
    }

    public void editPost(String postId, String newContent, ContentModerationSystem cms) {
        if (posts.contains(postId)) {
            cms.updatePostContent(postId, newContent);
        } else {
            System.out.println("You do not own this post.");
        }
    }

    public void deletePost(String postId, ContentModerationSystem cms) {
        if (posts.contains(postId)) {
            cms.deletePost(postId);
            posts.remove(postId);
        } else {
            System.out.println("You do not own this post.");
        }
    }

    public void appealFlaggedPost(String postId) {
        flaggedAppeals.put(postId, false);
        System.out.println("Appeal submitted for post ID: " + postId);
    }
}

// Admin class
class Admin extends Account {
    public Admin(String username, String password) {
        super(username, password);
    }

    public void moderateFlaggedPosts(ContentModerationSystem cms) {
        List<Post> flaggedPosts = cms.getFlaggedPosts();
        for (Post post : flaggedPosts) {
            System.out.println(post);
            System.out.println("Approve (1) or Reject (2)?");
            Scanner sc = new Scanner(System.in);
            int choice = sc.nextInt();
            if (choice == 1) {
                post.flagStatus = false;
                System.out.println("Post approved.");
            } else {
                cms.deletePost(post.postId);
                System.out.println("Post rejected and deleted.");
            }
        }
    }

    public void addFlaggedWord(String word, ContentModerationSystem cms) {
        cms.addUserDefinedFlaggedWord(word);
        System.out.println("Flagged word added: " + word);
    }

    public void removeFlaggedWord(String word, ContentModerationSystem cms) {
        cms.removeFlaggedWord(word);
        System.out.println("Flagged word removed: " + word);
    }

    public void viewAllPosts(ContentModerationSystem cms) {
        cms.displayAllPosts();
    }

    public void manageUsers(ContentModerationSystem cms) {
        cms.displayUsers();
    }

    public void monitorAppeals(ContentModerationSystem cms) {
        cms.displayAppeals();
    }
}

class Post {
    String postId;
    String content;
    boolean flagStatus;

    public Post(String postId, String content) {
        this.postId = postId;
        this.content = content;
        this.flagStatus = false;
    }

    @Override
    public String toString() {
        return "Post ID: " + postId + "\nContent: " + content + "\nFlag Status: " + (flagStatus ? "Flagged" : "Safe");
    }
}

// Trie Node class for storing flagged words
class TrieNode {
    HashMap<Character, TrieNode> children = new HashMap<>();
    boolean isEndOfWord = false;
}

class Trie {
    private TrieNode root;

    public Trie() {
        root = new TrieNode();
    }

    // Insert a word into the Trie
    public void insert(String word) {
        TrieNode current = root;
        for (char c : word.toLowerCase().toCharArray()) {
            current.children.putIfAbsent(c, new TrieNode());
            current = current.children.get(c);
        }
        current.isEndOfWord = true;
    }

    // Search for any flagged word in a given content
    public boolean containsFlaggedWord(String content) {
        String[] words = content.toLowerCase().split("\\W+");
        for (String word : words) {
            if (searchWord(word)) {
                return true;
            }
        }
        return false;
    }

    // Helper method to search for a complete word in the Trie
    private boolean searchWord(String word) {
        TrieNode current = root;
        for (char c : word.toCharArray()) {
            if (!current.children.containsKey(c)) {
                return false;
            }
            current = current.children.get(c);
        }
        return current.isEndOfWord;
    }

    // Remove a word from the Trie
    public boolean remove(String word) {
        return remove(root, word.toLowerCase(), 0);
    }

    private boolean remove(TrieNode current, String word, int index) {
        if (index == word.length()) {
            // Base case: end of word reached
            if (!current.isEndOfWord) {
                return false; // Word not found
            }
            current.isEndOfWord = false; // Unmark the end of word
            return current.children.isEmpty(); // If no children, node can be deleted
        }

        char c = word.charAt(index);
        TrieNode node = current.children.get(c);
        if (node == null) {
            return false; // Word not found
        }

        boolean shouldDeleteCurrentNode = remove(node, word, index + 1);

        if (shouldDeleteCurrentNode) {
            current.children.remove(c); // Remove the reference to the child
            return current.children.isEmpty() && !current.isEndOfWord;
        }

        return false;
    }
}

// Content Moderation System class
public class ContentModerationSystem {
    private HashMap<String, Account> accounts = new HashMap<>();
    private HashMap<String, Post> posts = new HashMap<>();
    private Trie flaggedKeywords = new Trie();

    // Register a new account
    public void registerUser(String username, String password) {
        accounts.put(username, new User(username, password));
        System.out.println("User registered: " + username);
    }

    public void registerAdmin(String username, String password) {
        accounts.put(username, new Admin(username, password));
        System.out.println("Admin registered: " + username);
    }

    // Login account
    public Account login(String username, String password) {
        Account account = accounts.get(username);
        if (account != null && account.password.equals(password)) {
            return account;
        }
        System.out.println("Invalid credentials.");
        return null;
    }

    // Add a new post
    public void addPost(String postId, String content, User user) {
        Post post = new Post(postId, content);
        if (flaggedKeywords.containsFlaggedWord(content)) {
            post.flagStatus = true;
        }
        posts.put(postId, post);
    }

    // Get a specific post
    public Post getPost(String postId) {
        return posts.get(postId);
    }

    // Update post content
    public void updatePostContent(String postId, String newContent) {
        Post post = posts.get(postId);
        if (post != null) {
            post.content = newContent;
            post.flagStatus = flaggedKeywords.containsFlaggedWord(newContent);
        }
    }

    // Delete a post
    public void deletePost(String postId) {
        posts.remove(postId);
    }

    // Get flagged posts
    public List<Post> getFlaggedPosts() {
        List<Post> flaggedPosts = new ArrayList<>();
        for (Post post : posts.values()) {
            if (post.flagStatus) {
                flaggedPosts.add(post);
            }
        }
        return flaggedPosts;
    }

    // Add flagged word
    public void addUserDefinedFlaggedWord(String word) {
        flaggedKeywords.insert(word);
    }

    public void removeFlaggedWord(String word) {
        boolean removed = flaggedKeywords.remove(word);
        if (removed) {
            System.out.println("Flagged word removed: " + word);
        } else {
            System.out.println("Word not found: " + word);
        }
    }

    // Display all posts
    public void displayAllPosts() {
        for (Post post : posts.values()) {
            System.out.println(post);
            System.out.println("---------------");
        }
    }

    // Display users
    public void displayUsers() {
        for (Account account : accounts.values()) {
            if (account instanceof User) {
                System.out.println("User: " + account.username);
            }
        }
    }

    // Display appeals
    public void displayAppeals() {
        for (Account account : accounts.values()) {
            if (account instanceof User) {
                User user = (User) account;
                if (!user.flaggedAppeals.isEmpty()) {
                    System.out.println("User: " + user.username);
                    System.out.println("Appeals: " + user.flaggedAppeals);
                }
            }
        }
    }

    public static void main(String[] args) {
        ContentModerationSystem cms = new ContentModerationSystem();

        // Register users and admins
        cms.registerUser("john_doe", "password123");
        cms.registerAdmin("admin1", "adminpass");

        // Example usage
        Account user = cms.login("john_doe", "password123");
        if (user instanceof User) {
            User u = (User) user;
            u.createPost("post1", "This is a test post.", cms);
            u.viewOwnPosts(cms);
            u.appealFlaggedPost("post1");
        }

        Account admin = cms.login("admin1", "adminpass");
        if (admin instanceof Admin) {
            Admin a = (Admin) admin;
            a.viewAllPosts(cms);
            a.moderateFlaggedPosts(cms);
        }
    }
}
