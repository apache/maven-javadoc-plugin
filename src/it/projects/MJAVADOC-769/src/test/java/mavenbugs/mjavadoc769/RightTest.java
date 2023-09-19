/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mavenbugs.mjavadoc769;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

public class RightTest {

  @Test
  public void testFoo() throws Exception {
    Method method = Foo.class.getMethod("foo");
    Annotation annotation = method.getAnnotation(Right.class);
    assertNotNull(annotation);
  }

  @Test
  public void testRight() throws Exception {
    Annotation[] annotations = Right.class.getAnnotations();
    assertEquals(4, annotations.length);

    assertEquals(javax.inject.Qualifier.class, Right.class.getAnnotation(javax.inject.Qualifier.class).annotationType());
    assertEquals(jakarta.inject.Qualifier.class, Right.class.getAnnotation(jakarta.inject.Qualifier.class).annotationType());
  }


  public static class Foo {
    @Right
    public void foo() {
    }
  }
}
