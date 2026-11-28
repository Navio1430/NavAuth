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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.spcode.navauth.api.event.NavAuthEvent;
import pl.spcode.navauth.api.event.NavAuthEventBus;
import pl.spcode.navauth.api.event.NavAuthEventListener;
import pl.spcode.navauth.api.event.Subscribe;

@Singleton
public class NavAuthEventBusInternal implements NavAuthEventBus {

  private final Logger logger = LoggerFactory.getLogger(NavAuthEventBusInternal.class);

  /**
   * Invocations per event class, ordered by priority (higher first); equal priorities keep their
   * registration order. Writers lock the list; {@link #post} iterates a snapshot without locking.
   */
  private final Map<Class<?>, CopyOnWriteArrayList<Invocation>> listeners =
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
        listeners.compute(
            eventType,
            (k, list) -> {
              CopyOnWriteArrayList<Invocation> target =
                  list != null ? list : new CopyOnWriteArrayList<>();
              insertByPriority(target, inv);
              return target;
            });
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
            CopyOnWriteArrayList<Invocation> list = listeners.get(eventType);
            if (list != null) {
              synchronized (list) {
                list.removeIf(inv -> listener.equals(inv.getTarget()));
              }
            }
          });
      cleanupEmpty();
    }
  }

  public void post(NavAuthEvent event) {
    Class<?> clazz = event.getClass();
    for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
      CopyOnWriteArrayList<Invocation> list = listeners.get(clazz);
      if (list != null) {
        // the iterator is a snapshot in priority order; listeners may (un)register meanwhile
        for (Invocation inv : list) {
          Object target = inv.getTarget();
          if (target == null) {
            synchronized (list) {
              list.remove(inv);
            }
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

  /**
   * Inserts {@code inv} after every invocation with the same or a higher priority, so higher
   * priorities run first and equal priorities run in registration order.
   */
  private static void insertByPriority(List<Invocation> list, Invocation inv) {
    synchronized (list) {
      int index = list.size();
      while (index > 0 && list.get(index - 1).getPriority() < inv.getPriority()) {
        index--;
      }
      list.add(index, inv);
    }
  }

  private void cleanupEmpty() {
    listeners.entrySet().removeIf(e -> e.getValue().isEmpty());
  }
}
