package com.printdoc.api.job;

import com.printdoc.api.shop.Shop;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.assertThat;

class JobServiceTest {
    @Test void storesMoneyRatesAsMinorUnits(){
        Shop shop=new Shop("TEST_SHOP","Sample Shop");
        assertThat(shop.getBwRate()).isEqualTo(200);
        assertThat(shop.getColorRate()).isEqualTo(500);
        assertThat(shop.getDuplexRate()).isEqualTo(300);
    }
    @Test void pageRangeAllowsSafeSyntaxOnly(){
        Pattern safe=Pattern.compile("(?i)all|[1-9][0-9]{0,3}(?:-[1-9][0-9]{0,3})?");
        assertThat(safe.matcher("all").matches()).isTrue();
        assertThat(safe.matcher("1-12").matches()).isTrue();
        assertThat(safe.matcher("../secret").matches()).isFalse();
        assertThat(safe.matcher("0-2").matches()).isFalse();
    }
}
