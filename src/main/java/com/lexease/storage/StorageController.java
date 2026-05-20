package com.lexease.storage;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/storage")
public class StorageController {
    private final ObjectStorageService objectStorageService;

    public StorageController(ObjectStorageService objectStorageService) {
        this.objectStorageService = objectStorageService;
    }

    @GetMapping("/local")
    ResponseEntity<byte[]> getLocalObject(
            @RequestParam("key") String key,
            @RequestParam("expires") long expires,
            @RequestParam("signature") String signature
    ) {
        StoredObject object = objectStorageService.readSignedObject(key, expires, signature);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(object.mimeType()))
                .cacheControl(CacheControl.noStore())
                .body(object.content());
    }
}
