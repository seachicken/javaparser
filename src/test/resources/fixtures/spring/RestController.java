package fixtures.spring;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/path")
public class RestController {
    @GetMapping
    public ResponseEntity getPath() {
        return ResponseEntity.ok();
    }
}
