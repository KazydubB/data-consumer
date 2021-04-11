package consumer;

import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import consumer.repository.CommentRepository;
import consumer.repository.IdAndTitle;
import consumer.service.StoryService;

@Component
public class AppRunner implements CommandLineRunner {

    private final NetRunner netRunner;
    private final StoryService storyService;

    public AppRunner(NetRunner netRunner, StoryService storyService, CommentRepository commentRepository) {
        this.netRunner = netRunner;
        this.storyService = storyService;
    }

    @Override
    public void run(String... args) throws Exception {
        while (true) {
            System.out.print("Type your command > ");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine().trim();

            // Available commands are:
            // - consume -- consumes data from endpoint and stores it into local DB;
            // - list -- shows all Story entities stored in the DB after `consume`;
            // - list id -- shows an entity with its parents and kids identified
            //              by id param (with id being integer value);
            // - exit/quit -- exit from the application.

            if (input.equalsIgnoreCase("consume")) {
                netRunner.run();
            } else if (input.equalsIgnoreCase("list")) {
                List<IdAndTitle> idAndTitles = storyService.findAllStoryIdAndTitlePairs();
                if (idAndTitles.isEmpty()) {
                    System.out.println("There are no entries to show.");
                } else {
                    // output Story entries
                    System.out.printf("Showing %d entries:\n", idAndTitles.size());
                    for (IdAndTitle entry : idAndTitles) {
                        System.out.println(entry.getExternalId() + " " + entry.getTitle());
                    }
                }
            } else if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                netRunner.clearQueues();
                break;
            } else {
                String[] command = input.split("\\s+");
                Pattern integerPattern = Pattern.compile("\\d+");
                if (command.length == 2 && command[0].equalsIgnoreCase("list") && integerPattern.matcher(command[1]).matches()) {
                    long id = Long.parseLong(command[1]);
                    String jsonEntity = storyService.getStoryJsonWithRelatedData(id);
                    if (jsonEntity == null) {
                        System.out.printf("Unable to find entity with id: %d\n", id);
                    } else {
                        System.out.println(jsonEntity);
                    }
                } else {
                    System.out.println("Available commands: `consume`, `list`, `list ID`, `exit`");
                }
            }
        }
    }
}
