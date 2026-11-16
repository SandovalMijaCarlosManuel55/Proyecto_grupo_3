package pe.edu.usat.incidentapp.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import pe.edu.usat.incidentapp.network.RetrofitClient;

/**
 * Utilidades para las fotos del incidente: archivo temporal para la cámara
 * y copia comprimida (JPEG) que se envía al API o se guarda para modo offline.
 */
public class ImagenHelper {
    private static final int LADO_MAXIMO = 1280;

    private static File carpetaFotos(Context context) {
        File carpeta = new File(context.getFilesDir(), "fotos");
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }
        return carpeta;
    }

    //Origen para Glide: las fotos pueden ser links de internet o archivos subidos al API (requieren el JWT)
    public static Object origenFoto(String foto) {
        if (foto.startsWith("http")) {
            return foto;
        }
        return new GlideUrl(RetrofitClient.URL_API_SERVICE + "incidentes/fotos/" + foto,
                new LazyHeaders.Builder()
                        .addHeader("Authorization", "Bearer " + SessionManager.getInstance().getToken())
                        .build());
    }

    //Uri donde la cámara guardará la foto tomada
    public static Uri crearUriCamara(Context context) {
        File archivo = new File(carpetaFotos(context), "camara_" + System.currentTimeMillis() + ".jpg");
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", archivo);
    }

    //Copia la imagen reducida a máximo 1280px en JPEG y devuelve la ruta del archivo
    public static String comprimir(Context context, Uri uri) throws IOException {
        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, opciones);
        }

        int escala = 1;
        while (opciones.outWidth / escala > LADO_MAXIMO * 2 || opciones.outHeight / escala > LADO_MAXIMO * 2) {
            escala *= 2;
        }

        BitmapFactory.Options decodificar = new BitmapFactory.Options();
        decodificar.inSampleSize = escala;
        Bitmap bitmap;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(in, null, decodificar);
        }
        if (bitmap == null) {
            throw new IOException("No se pudo leer la imagen");
        }

        float factor = Math.min(1f, (float) LADO_MAXIMO / Math.max(bitmap.getWidth(), bitmap.getHeight()));
        if (factor < 1f) {
            Bitmap reducido = Bitmap.createScaledBitmap(bitmap,
                    Math.round(bitmap.getWidth() * factor), Math.round(bitmap.getHeight() * factor), true);
            bitmap.recycle();
            bitmap = reducido;
        }

        File destino = new File(carpetaFotos(context), "foto_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream out = new FileOutputStream(destino)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
        }
        bitmap.recycle();
        return destino.getAbsolutePath();
    }
}
