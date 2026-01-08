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

import com.google.inject.Singleton;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.spcode.navauth.api.event.NavAuthEvent;
import pl.spcode.navauth.api.event.NavAuthEventBus;
import pl.spcode.navauth.api.event.NavAuthEventListener;
import pl.spcode.navauth.api.event.Subscribe;

@Singleton
public class NavAuthEventBusInternal implements NavAuthEventBus {

  private final Logger logger = LoggerFactory.getLogger(NavAuthEventBusInternal.class);

  private final Map<Class<?>, PriorityBlockingQueue<Invocation>> listeners =
      new ConcurrentHashMap<>();
  private final Map<Object, Set<Class<?>>> registrations = new ConcurrentHashMap<>();

  @Override
  public void register(NavAuthEventListener listener) {
    Class<?> cls = listener.getClass();
    List<Method> methods =
        Arrays.stream(cls.getDeclaredMethods())
            .filter(m -> m.isAnnotationPresent(Subscribe.class))
            .filter(m -> m.getParameterCount() == 1)
            .peek(m -> m.setAccessible(true))
            .toList();

    Set<Class<?>> types = new HashSet<>();
    for (Method method : methods) {
      Class<?> eventType = method.getParameterTypes()[0];
      Subscribe sub = method.getAnnotation(Subscribe.class);
      try {
        Invocation inv = new MethodInvocation(method, listener, sub.priority());
        listeners.computeIfAbsent(eventType, k -> new PriorityBlockingQueue<>()).add(inv);
        types.add(eventType);
      } catch (Exception e) {
        logger.error("Failed to register method", e);
      }
    }
    registrations.put(listener, types);
  }

  @Override
  public void unregister(NavAuthEventListener listener) {
    Set<Class<?>> types = registrations.remove(listener);
    if (types != null) {
      types.forEach(
          eventType -> {
            PriorityBlockingQueue<Invocation> queue = listeners.get(eventType);
            if (queue != null) {
              queue.removeIf(inv -> listener.equals(inv.getTarget()));
            }
          });
      cleanupEmpty();
    }
  }

  public void post(NavAuthEvent event) {
    Class<?> clazz = event.getClass();
    for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
      PriorityBlockingQueue<Invocation> queue = listeners.get(clazz);
      if (queue != null) {
        Iterator<Invocation> it = queue.iterator();
        while (it.hasNext()) {
          Invocation inv = it.next();
          Object target = inv.getTarget();
          if (target == null) {
            it.remove();
            continue;
          }
          try {
            inv.invoke(event);
          } catch (Exception e) {
            logger.error("Failed to invoke event listener method", e);
          }
        }
      }
    }
    cleanupEmpty();
  }

  private void cleanupEmpty() {
    listeners.entrySet().removeIf(e -> e.getValue().isEmpty());
  }
}
