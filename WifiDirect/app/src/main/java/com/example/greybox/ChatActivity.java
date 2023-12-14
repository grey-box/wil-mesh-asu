package com.example.greybox;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.example.greybox.meshmessage.FilePayload;
import com.example.greybox.meshmessage.MeshMessage;
import com.example.greybox.meshmessage.MeshMessageType;
import com.example.greybox.netservice.NetService;
import com.github.bassaer.chatmessageview.model.ChatUser;
import com.github.bassaer.chatmessageview.view.ChatView;
import com.github.bassaer.chatmessageview.model.Message;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import android.Manifest;

import org.apache.commons.io.IOUtils;


public class ChatActivity extends AppCompatActivity {

    private ChatView chatView;
    private ChatUser localUser; // L'utilisateur local
    private ChatUser remoteUser; // L'utilisateur distant
    private UUID msgDstDeviceId; // UUID du dispositif  sélectionné
    private NetService mNetService;
    private static final String TAG = "ChatActivity";
    Handler uiHandler;
    private ArrayList<UUID> groupDeviceIds;
    private Uri selectedFileUri;
    private byte[] selectedFileBytes;
    private static final int REQUEST_CODE_PICK_FILE = 100;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 101;


    // Définissez un launcher pour gérer le résultat de l'intention de choix de fichier
    // Ajouter en tant que membre de classe
    private final ActivityResultLauncher<Intent> fileChooserLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();

                    // Traiter le fichier ici
                    showImagePreview(fileUri);
                }
            }
    );

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

       // Vérifier et demander la permission d'écriture sur le stockage externe
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
        // Trouvez l'ImageButton et définissez un OnClickListener
        ImageButton buttonChooseFile = findViewById(R.id.button_choose_file);
        buttonChooseFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseFile(); // Appelle la méthode pour choisir un fichier
            }
        });


        // Initialisation du Handler pour gérer les messages entrants
        uiHandler = new Handler(getMainLooper(), message -> {
            if (message.what == ThreadMessageTypes.MESSAGE_READ) {
                mNetService.handleThreadMessage(message);
            }
            // Autres cas
            return true;
        });

        // Configuration du listener pour le bouton d'envoi de message
        chatView.setOnClickSendButtonListener(view -> onSendMessageRequested());

        // Configuration du callback pour la mise à jour des messages entrants
        mNetService.setMessageTextUiCallback(msgText -> {
            Message receivedMessage = new Message.Builder()
                    .setUser(remoteUser)
                    .setRight(false)
                    .setText(msgText)
                    .build();
            chatView.receive(receivedMessage);
        });

        // Configuration du callback pour les fichiers reçus
        mNetService.setFileReceivedUiCallback((fileBytes, fileName) -> runOnUiThread(() -> {
            Log.d(TAG, "File received: " + fileName);
            if (fileName.toLowerCase().endsWith(".png") || fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.length);
                    // Utilisez la méthode displayImageMessage pour afficher l'image dans le ChatView
                    displayImageMessage(bitmap, false, remoteUser); // false car c'est le message du partenaire distant
                } catch (Exception e) {
                    Log.e("ChatActivity", "Erreur lors de la décodage de l'image: " + e.getMessage());
                }
            } else {
                saveFile(fileBytes, fileName); // Pour les fichiers non-image
            }
        }));

    }

    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            return MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveFile(byte[] fileBytes, String fileName) {
        File outputFile = new File(getExternalFilesDir(null), fileName);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(fileBytes);
            Toast.makeText(this, "Fichier enregistré: " + outputFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur lors de l'enregistrement du fichier", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Autres 'case' si nécessaire
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // La permission a été accordée
            } else {
                // La permission a été refusée
                Toast.makeText(this, "Permission refusée", Toast.LENGTH_SHORT).show();
            }
    }

    private void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        fileChooserLauncher.launch(intent); // Utilisez le launcher pour démarrer l'intention
    }


   private void showImagePreview(Uri imageUri) {
        ImageView imagePreview = findViewById(R.id.image_preview);
        imagePreview.setVisibility(View.VISIBLE);

        // Redimensionner l'image pour l'aperçu
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (bitmap != null) {
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true); // Ajustez la taille selon le besoin
            imagePreview.setImageBitmap(scaledBitmap);
        } else {
            imagePreview.setImageURI(imageUri); // Si la redimension échoue, utiliser l'URI directement
        }
        // Stockez l'URI ou les données du fichier pour un usage ultérieur
        selectedFileUri = imageUri;
        selectedFileBytes = readFile(imageUri);
    }


    private byte[] readFile(Uri fileUri) {
        try (InputStream inputStream = getContentResolver().openInputStream(fileUri)) {
            assert inputStream != null;
            return IOUtils.toByteArray(Objects.requireNonNull(inputStream)); // Utiliser Apache Commons IO
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    // Méthode déclenchée par l'événement d'envoi de message du ChatView


    private void onSendMessageRequested() {
        String localDeviceUUIDString = getIntent().getStringExtra("LOCAL_DEVICE_UUID");
        UUID localDeviceUUID = localDeviceUUIDString != null ? UUID.fromString(localDeviceUUIDString) : null;

        if (selectedFileBytes != null && selectedFileUri != null) {
            Bitmap imageBitmap = getBitmapFromUri(selectedFileUri);
            if (imageBitmap != null) {
                displayImageMessage(imageBitmap, true, localUser); // Afficher l'image dans le ChatView
            }

            if (groupDeviceIds.size() == 1) { // Chat individuel
                sendFile(selectedFileBytes, msgDstDeviceId); // Envoyer directement au destinataire individuel
            } else { // Chat de groupe
                sendFileToAllDevicesExceptLocal(selectedFileBytes, localDeviceUUID);
            }

            // Réinitialiser les variables de fichier sélectionné
            selectedFileBytes = null;
            selectedFileUri = null;
        } else {
            String messageText = chatView.getInputText();
            if (!messageText.isEmpty()) {
                if (groupDeviceIds.size() == 1) { // Chat individuel
                    MeshMessage meshMessage = new MeshMessage(
                            MeshMessageType.DATA_SINGLE_CLIENT,
                            messageText,
                            new ArrayList<>(Collections.singletonList(msgDstDeviceId))
                    );
                    mNetService.sendMessage(meshMessage);
                    Log.d("ChatActivity", "Sending text message to UUID: " + msgDstDeviceId);
                } else { // Chat de groupe
                    sendMessageToAllDevicesExceptLocal(messageText, localDeviceUUID);
                }
                displayMessage(localUser, messageText, true);
            }
        }
    }




    private void sendFileToAllDevicesExceptLocal(byte[] fileBytes, UUID localDeviceUUID) {
        Log.d(TAG, "UUID Local: " + localDeviceUUID);
        Log.d("ChatActivity", "Group deviceId "+ groupDeviceIds );
        for (UUID deviceId : groupDeviceIds) {
            if (!deviceId.equals(localDeviceUUID)) {
                sendFile(fileBytes, deviceId);
                Log.d("ChatActivity", "Sending file to UUID: " + deviceId);
            }
        }
    }

    private void sendMessageToAllDevicesExceptLocal(String messageText, UUID localDeviceUUID) {
        for (UUID deviceId : groupDeviceIds) {
            if (!deviceId.equals(localDeviceUUID)) {
                MeshMessage meshMessage = new MeshMessage(
                        MeshMessageType.DATA_SINGLE_CLIENT,
                        messageText,
                        new ArrayList<>(Collections.singletonList(deviceId))
                );
                mNetService.sendMessage(meshMessage);
                Log.d("ChatActivity", "Sending text message to UUID: " + deviceId);
            }
        }
    }



    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String result = null;
        if (Objects.equals(uri.getScheme(), "content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            assert result != null;
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }



    private void sendFile(byte[] fileBytes, UUID destinationUUID) {
        Log.d(TAG, "Sending file to UUID: " + destinationUUID);
        String fileName = getFileName(selectedFileUri);
        FilePayload filePayload = new FilePayload(fileBytes, fileName);

        MeshMessage fileMessage = new MeshMessage(
                MeshMessageType.FILE_TRANSFER,
                filePayload,
                new ArrayList<>(Collections.singletonList(destinationUUID))
        );
        mNetService.sendMessage(fileMessage);
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
    private void displayImageMessage(Bitmap picture, boolean isRightSide, ChatUser user) {
        Message message = new Message.Builder()
                .setRight(isRightSide)
                .setUser(user)
                .setPicture(picture) // Définissez l'image ici
                .setType(Message.Type.PICTURE) // Définissez le type de message comme image
                .build();

        chatView.receive(message); // Ajoutez le message à la vue de chat
    }


}

