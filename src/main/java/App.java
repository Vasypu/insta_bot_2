import database.DatabaseConnector;
import entity.Post;
import entity.User;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import javax.persistence.EntityManagerFactory;
import java.io.File;
import java.io.IOException;

public class App extends TelegramLongPollingBot {
    private DatabaseConnector connector;

    public App(DatabaseConnector connector) { this.connector = connector; }

    @Override
    public void onUpdateReceived(Update update) {
        Long userId = update.getMessage().getChatId();
        connector.startTransaction();
        User user = connector.getUserService().findById(userId);
        System.out.println("Message " + update.getMessage().getText());
        if (user == null) {
            try {
                execute(new SendMessage(update.getMessage().getChatId(),
                        "Вам необходимо прислать логин и пароль в одном предложении через пробел"));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            connector.getUserService().save(new User(update.getMessage().getChatId(), null, null));
        } else if (user.getLogin() == null || user.getPassword() == null) { // запись логина и пароля

            String[] loginAndPassword = update.getMessage().getText().split(" ");
            user.setLogin(loginAndPassword[0]);
            user.setPassword(loginAndPassword[1]);

            connector.getUserService().save(user);

            try {
                execute(new SendMessage(update.getMessage().getChatId(),
                        "Все работает! Теперь вы можете присылать нам текст/изображение для Instagram (в одном сообщении)"));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else if (user.getLogin() != null && update.getMessage().getPhoto().size() > 0) {
            GetFile file = new GetFile().setFileId(update.getMessage().getPhoto().get(0).getFileId());
            String filePath = null;
            try {
                filePath = execute(file).getFilePath();
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            String downPath = "https://api.telegram.org/file/bot"+ getBotToken() + "/" + filePath;
            try {
                HttpDownload.downloadFile(downPath, "./images", update.getMessage().getPhoto().get(0).getFileId() + ".jpg");
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }

            Post post = new Post();
            post.setTitle(update.getMessage().getCaption());
            post.setPhoto(new File("./images/" + update.getMessage().getPhoto().get(0).getFileId() + ".jpg").getPath());
            user.addPost(post);

            connector.getPostService().save(post);
            connector.getUserService().save(user);
        }
        connector.endTransaction();
    }

    @Override
    public String getBotUsername() { return "second_insta_bot"; }

    @Override
    public String getBotToken() { return "1599893568:AAG3q_WkZZnTmV5sJRTIUp3BHsnE8XsrHwE"; }
}