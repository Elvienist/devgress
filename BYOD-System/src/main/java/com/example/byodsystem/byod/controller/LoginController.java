package com.example.byodsystem.byod.controller;

import org.mindrot.jbcrypt.BCrypt;
import com.example.byodsystem.byod.database.DBConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static com.example.byodsystem.byod.database.DBConnection.*;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    public void login() {

        try {

            Connection conn = connect();

            String sql = "SELECT * FROM users WHERE username=?";


            PreparedStatement pst = conn.prepareStatement(sql);

            pst.setString(1, usernameField.getText());

            ResultSet rs = pst.executeQuery();

            if(rs.next()) {
                String hashedPassword = rs.getString("password_hash");
                if(!BCrypt.checkpw(passwordField.getText(), hashedPassword)) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText("Invalid Credentials");
                    alert.show();
                    return;
                }

                Parent root = FXMLLoader.load(getClass().getResource("/com/example/byodsystem/byod/fxml/dashboard.fxml"));

                Stage stage = (Stage) usernameField.getScene().getWindow();

                stage.setScene(new Scene(root));

            } else {

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Invalid Credentials");
                alert.show();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
