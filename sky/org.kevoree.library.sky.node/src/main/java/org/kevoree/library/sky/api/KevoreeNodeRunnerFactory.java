package org.kevoree.library.sky.api;

import org.kevoree.library.sky.api.execution.KevoreeNodeRunner;

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 25/06/13
 * Time: 11:06
 *
 * @author Erwan Daubert
 * @version 1.0
 */
public interface KevoreeNodeRunnerFactory {
    KevoreeNodeRunner createKevoreeNodeRunner(String nodeName);
}
