package com.example.greybox;

import static com.example.greybox.MainActivity.sNetService;
import static com.example.greybox.MainActivity.sgroupClientsList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.greybox.R;
import com.example.greybox.meshmessage.MeshMessage;
import com.example.greybox.meshmessage.MeshMessageType;
import com.example.greybox.netservice.NetService;
import com.github.bassaer.chatmessageview.model.ChatUser;
import com.github.bassaer.chatmessageview.model.Message;
import com.github.bassaer.chatmessageview.view.ChatView;


import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import android.os.Handler;



public class ChatActivity extends AppCompatActivity {

    private ChatView chatView;
    private ChatUser localUser; // L'utilisateur local
    private ChatUser remoteUser; // L'utilisateur distant
    private UUID msgDstDeviceId; // UUID du dispositif  sélectionné
    private NetService mNetService;
    private static final String TAG = "ChatActivity";
    Handler uiHandler;

    private ArrayList<UUID> groupDeviceIds;
    android.os.Message message = new android.os.Message();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatView = findViewById(R.id.chat_view);
        Bitmap defaultIcon = BitmapFactory.decodeResource(getResources(), R.drawable.face_1);
        localUser = new ChatUser(1, "Moi", defaultIcon);

        ArrayList<String> groupDeviceIdsStrings = getIntent().getStringArrayListExtra("GROUP_DEVICE_IDS");
        if (groupDeviceIdsStrings != null && !groupDeviceIdsStrings.isEmpty()) {
            Log.d("ChatActivity", "In Group Chat mode with UUIDs: " + groupDeviceIdsStrings);
            groupDeviceIds = new ArrayList<>();
            for (String uuidString : groupDeviceIdsStrings) {
                groupDeviceIds.add(UUID.fromString(uuidString));
            }
            remoteUser = new ChatUser(2, "Group Chat", defaultIcon);
        } else {
            String deviceName = getIntent().getStringExtra("REMOTE_DEVICE_NAME");
            String deviceId = getIntent().getStringExtra("DEVICE_ID");
            Log.d("ChatActivity", "In Individual Chat mode with device: " + deviceName + ", UUID: " + deviceId);
            if (deviceId == null) {
                Toast.makeText(this, "deviceId du dispositif distant manquant.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            remoteUser = new ChatUser(2, deviceName != null ? deviceName : "Unknown Device", defaultIcon);
            msgDstDeviceId = UUID.fromString(deviceId);
            groupDeviceIds = new ArrayList<>(Collections.singletonList(msgDstDeviceId));
        }

        mNetService = MainActivity.sNetService;


    // Initialisation du Handler pour gérer les messages entrants
        uiHandler = new Handler(getMainLooper(), message -> {
            if (message.what == ThreadMessageTypes.MESSAGE_READ) {
                MeshMessage meshMsg = (MeshMessage) message.obj;
                mNetService.handleThreadMessage(message);
            }
            // Autres cas
            return true;
        });

        // Configuration du listener pour le bouton d'envoi de message
        chatView.setOnClickSendButtonListener(view -> onSendMessageRequested(chatView.getInputText()));

        // Configuration du callback pour la mise à jour des messages entrants
        mNetService.setMessageTextUiCallback(msgText -> {
            Message receivedMessage = new Message.Builder()
                    .setUser(remoteUser)
                    .setRight(false)
                    .setText(msgText)
                    .build();
            chatView.receive(receivedMessage);
        });
    }




    NetService.MessageTextUiCallback messageTextUiCallback = new NetService.MessageTextUiCallback() {
        @Override
        public void updateMessageTextUiCallback(String msgText) {
            Message receivedMessage;
            receivedMessage = new Message.Builder()
                    .setUser(remoteUser)
                    .setRight(false)
                    .setText(msgText)
                    .build();
            chatView.receive(receivedMessage);

        }
    };

    // Méthode déclenchée par l'événement d'envoi de message du ChatView


    private void onSendMessageRequested(String messageText) {
        // Récupérer l'UUID de l'appareil local de l'intention
        String localDeviceUUIDString = getIntent().getStringExtra("LOCAL_DEVICE_UUID");
        UUID localDeviceUUID = localDeviceUUIDString != null ? UUID.fromString(localDeviceUUIDString) : null;

        for (UUID deviceId : groupDeviceIds) {
            // Vérifier si l'UUID n'est pas celui de l'expéditeur
            if (!deviceId.equals(localDeviceUUID)) {
                MeshMessage meshMessage = new MeshMessage(
                        MeshMessageType.DATA_SINGLE_CLIENT,
                        messageText,
                        new ArrayList<>(Collections.singletonList(deviceId))
                );
                mNetService.sendMessage(meshMessage);
                Log.d("ChatActivity", "Sending message to UUID: " + deviceId);
            }
        }

        // Afficher le message dans l'interface utilisateur
        displayMessage(localUser, messageText, true);
    }



    private void displayMessage(ChatUser user, String messageText, boolean isRightSide) {
        runOnUiThread(() -> {
            Message message = new Message.Builder()
                    .setUser(user)
                    .setRight(isRightSide)
                    .setText(messageText)
                    .build();
            chatView.receive(message);
        });
    }

}

