package g2.be.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class XmlCombinedFlatRecordId implements Serializable {
    private String maLk;
    private Integer sttXml4;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XmlCombinedFlatRecordId that = (XmlCombinedFlatRecordId) o;
        return Objects.equals(maLk, that.maLk) &&
               Objects.equals(sttXml4, that.sttXml4);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maLk, sttXml4);
    }
}
