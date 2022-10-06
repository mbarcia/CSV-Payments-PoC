import poc.Command;
import poc.csvfolder.CSVFolder;
import poc.csvfolder.ReadFolderCommand;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Processable object creation
        CSVFolder aFolder = new CSVFolder("/Users/mari/IdeaProjects/CSV Payments PoC/test/files");
        // Commands creation
        Command<CSVFolder> readFolderCommand = new ReadFolderCommand();
        // Commands list assignment
        aFolder.setCommandList(List.of(readFolderCommand));
        // Make it all happen
        aFolder.forEach(command -> command.execute(aFolder));
    }
}
