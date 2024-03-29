package minderengine;

import org.interop.xoola.core.XoolaProperty;

import java.util.HashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This class has instances per session. It holds the values of signal calls that have occurred on the wrappers' side.
 *
 * Created by yerlibilgin on 02/12/14.
 */
public class MinderSignalRegistry {
  private HashMap<String, HashMap<String, PriorityBlockingQueue<SignalData>>> wrapperMap = new HashMap<String, HashMap<String, PriorityBlockingQueue<SignalData>>>();

  /**
   * A signal was emitted on the wrapper side. We should put it into the queue until it gets taken by a rivet.
   * @param label
   * @param signature
   * @param signalData
   */
  public void enqueueSignal(String label, String signature, SignalData signalData) {
    PriorityBlockingQueue<SignalData> queue = initMap(label, signature);
    queue.offer(signalData);
  }

  /**
   * If the signal is not yet emitted, we still have to settle down
   * and wait on a queue. That is why, we have to call init-map method here too
   * @param label
   * @param signature
   * @param timeout maximum timeout to wait for the deque operation. 0 means get the default (XoolaProperty.NETWORK_RESPONSE_TIMEOUT)
   *                from app.conf.
   * @return
   */
  public SignalData dequeueSignal(String label, String signature, long timeout){
    PriorityBlockingQueue<SignalData> queue = initMap(label, signature);

    if(timeout == 0)
      timeout = Long.parseLong(XoolaServer.properties.getProperty(XoolaProperty.NETWORK_RESPONSE_TIMEOUT));

    try {
      SignalData result = queue.poll(timeout, TimeUnit.MILLISECONDS);
      if(result == null){
        throw new RuntimeException("Signal Timeout Expired (" + timeout + ")");
      }

//      if (result.error != null)
//        throw new RuntimeException("Signal " + label + "." + signature + " reported failure [" + result.error + "]");

      return result;
    } catch (InterruptedException e) {
      throw new RuntimeException("Signal dequeue operation cancelled");
    }
  }

  private PriorityBlockingQueue<SignalData> initMap(String label, String signature) {
    HashMap<String, PriorityBlockingQueue<SignalData>> signalMap = null;
    if (!wrapperMap.containsKey(label)){
      signalMap = new HashMap<String, PriorityBlockingQueue<SignalData>>();
      wrapperMap.put(label, signalMap);
    } else{
      signalMap = wrapperMap.get(label);
    }

    PriorityBlockingQueue<SignalData> queue = null;
    if (!signalMap.containsKey(signature)){
      queue = new PriorityBlockingQueue<SignalData>();
      signalMap.put(signature, queue);
    } else{
      queue = signalMap.get(signature);
    }
    return queue;
  }
}
