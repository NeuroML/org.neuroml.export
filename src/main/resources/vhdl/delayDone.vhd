----------------------------------------------------------------------------------
-- Company: 
-- Engineer: 
-- 
-- Create Date:    00:39:32 01/12/2015 
-- Design Name: 
-- Module Name:    delayDone - Behavioral 
-- Project Name: 
-- Target Devices: 
-- Tool versions: 
-- Description: 
--
-- Dependencies: 
--
-- Revision: 
-- Revision 0.01 - File Created
-- Additional Comments: 
--
----------------------------------------------------------------------------------
library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

-- Uncomment the following library declaration if using
-- arithmetic functions with Signed or Unsigned values
--use IEEE.NUMERIC_STD.ALL;

-- Uncomment the following library declaration if instantiating
-- any Xilinx primitives in this code.
--library UNISIM;
--use UNISIM.VComponents.all;

library ieee_proposed;
use ieee_proposed.fixed_pkg.all;
use IEEE.numeric_std.all;

entity delayDone is
	generic( 
		Steps 	: integer := 10);	
	port(
		clk		: In  Std_logic;
		rst		: In  Std_logic;
		Start		: In  Std_logic;
		Done		: Out  Std_logic
		);
end delayDone;

architecture Behavioral of delayDone is
signal count : integer ;
signal count_next : integer ;
begin


combinationProc : process (count,start)
begin
count_next <= count;
Done <= '0';
if start = '1' then
count_next <= 0;
elsif count < Steps then
count_next <= count + 1;
else
Done <= '1';
end if;
end process;

synchronousProc : process (clk,rst)
begin
if rst = '1' then
	count <= Steps;
elsif clk'event and clk = '1' then
	count <= count_next;
end if;
end process;



end Behavioral;


