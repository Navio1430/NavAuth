/*
 * NavAuth
 * Copyright © 2026 Oliwier Fijas (Navio1430)
 *
 * NavAuth is free software; You can redistribute it and/or modify it under the terms of:
 * the GNU Affero General Public License version 3 as published by the Free Software Foundation.
 *
 * NavAuth is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with NavAuth. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 *
 */

package pl.spcode.navauth.common.infra;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MethodInvocation implements Invocation, Comparable<MethodInvocation> {

  private static final Logger logger = LoggerFactory.getLogger(MethodInvocation.class);

  private final MethodHandle methodHandle;
  private final WeakReference<Object> targetRef;
  private final int priority;

  MethodInvocation(Method method, Object target, int priority) throws Exception {
    this.methodHandle = MethodHandles.lookup().unreflect(method);
    this.targetRef = new WeakReference<>(target);
    this.priority = priority;
  }

  @Override
  public void invoke(Object event) {
    Object target = targetRef.get();
    if (target == null) return;
    try {
      methodHandle.invoke(target, event);
    } catch (Throwable t) {
      logger.error("Error occurred while trying to invoke MethodInvocation", t);
    }
  }

  @Override
  public Object getTarget() {
    return targetRef.get();
  }

  @Override
  public int compareTo(MethodInvocation o) {
    return Integer.compare(o.priority, priority);
  }
}
