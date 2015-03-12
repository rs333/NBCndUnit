/*
 * NBCndUnit - C/C++ unit tests for NetBeans.
 * Copyright (C) 2015  offa
 * 
 * This file is part of NBCndUnit.
 *
 * NBCndUnit is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NBCndUnit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NBCndUnit.  If not, see <http://www.gnu.org/licenses/>.
 */

package bv.offa.netbeans.cnd.unittest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.openide.modules.ModuleInfo;
import org.openide.modules.ModuleInstall;
import org.openide.modules.Modules;

/**
 * The class {@code Installer} implements an installer which will add this
 * module as friend to other modules.
 * 
 * @author offa
 */
public class Installer extends ModuleInstall
{
    private static final long serialVersionUID = 1L;
    private final Set<String> targetModules;
    
    
    public Installer()
    {
        this.targetModules = new HashSet<String>();
        this.targetModules.add("org.netbeans.modules.gsf.testrunner");
        this.targetModules.add("org.netbeans.modules.cnd.testrunner");
    }

    
    
    /**
     * Called if the module is loaded. It adds this module to the friend-list of
     * specified modules. If there are no modules specified, this method
     * does nothing.
     * 
     * @throws IllegalStateException    Thrown if an exception occurs
     *                                  while loading 
     */
    @Override
    public void validate() throws IllegalStateException
    {
        if( targetModules.isEmpty() == false )
        {
            addFriends();
        }
    }
    
    
    /**
     * Adds additional friends to specified modules. The modules are specified
     * by their module code name base within {@code targetModules}.
     * 
     * @exception IllegalStateException     Will rethrow previous exceptions
     */
    private void addFriends()
    {
        try
        {
            final ModuleInfo moduleInfoOfThis = Modules.getDefault().ownerOf(this.getClass());
            assert(moduleInfoOfThis != null);
            final String codeNameBase = moduleInfoOfThis.getCodeNameBase();
            
            final Method getManagerMethod = moduleInfoOfThis.getClass().getMethod("getManager");
            final Object manager = getManagerMethod.invoke(moduleInfoOfThis);
            
            final Method getMethod = manager.getClass().getMethod("get", String.class);
            
            for( String target : targetModules )
            {
                Object dependency = getMethod.invoke(manager, target);
                assert(dependency != null);
                
                final ModuleInfo moduleInfo = (ModuleInfo) dependency;
                
                final Class<?> moduleClass = Class.forName("org.netbeans.Module", 
                                                            true, 
                                                            moduleInfo.getClass()
                                                                    .getClassLoader());
                final Method dataMethod = moduleClass.getDeclaredMethod("data");
                dataMethod.setAccessible(true);
                
                final Object dataValue = dataMethod.invoke(moduleInfo);
                
                final Class<?> moduleDataClass = Class.forName("org.netbeans.ModuleData", 
                                                                true, 
                                                                dataValue.getClass()
                                                                        .getClassLoader());
                final Field friendNamesField = moduleDataClass.getDeclaredField("friendNames");
                
                updateFriendsValue(friendNamesField, dataValue, codeNameBase);
            }
        }
        catch( ReflectiveOperationException ex )
        {
            throw new IllegalStateException(ex);
        }
        catch( SecurityException ex )
        {
            throw new IllegalStateException(ex);
        }
        catch( IllegalArgumentException ex )
        {
            throw new IllegalStateException(ex);
        }
    }
    
    
    /**
     * Adds the module {@code friendToAdd} to the {@code friendField} of
     * object {@code obj}.
     * 
     * @param friendField   Field of the friendlist
     * @param obj           Object to modify
     * @param friendToAdd   Module code name base to add as friend
     * @throws IllegalAccessException   On failure of reflection access
     */
    private void updateFriendsValue(Field friendField, Object obj, String friendToAdd)
            throws IllegalAccessException
    {
        friendField.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        Set<String> value = (Set<String>) friendField.get(obj);
        assert(value != null);
        
        Set<String> newValue = new HashSet<String>(value);
        newValue.add(friendToAdd);
        friendField.set(obj, newValue);
    }
}
