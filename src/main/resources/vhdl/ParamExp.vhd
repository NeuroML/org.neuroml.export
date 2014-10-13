--
-- Parameterisable N to M mux.
--

library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

-- For Modelsim
--use ieee.fixed_pkg.all;
--use ieee.fixed_float_types.ALL;

-- For ISE
library ieee_proposed;
use ieee_proposed.fixed_pkg.all;
use ieee_proposed.fixed_float_types.ALL;
use IEEE.numeric_std.all;

entity ParamExp is
	port(
		clk		: In  Std_logic;
		rst		: In  Std_logic;
		Start	: In  Std_logic;
		Done	: Out  Std_logic;
		X		: In sfixed(20 downto -20);
		Output	: Out sfixed(20 downto -20)
		);
end ParamExp;

architecture RTL of ParamExp is
type MEM is array (0 to 7) of sfixed(20 downto -20);
signal output_internal : sfixed(20 downto -20);
signal currentTerm : sfixed(20 downto -20);
signal COUNT : unsigned(3 downto 0) := "1001";
begin
	
	process(clk)
		variable MEM8Xsfixed : MEM := (to_sfixed (1,20, -20),to_sfixed (0.5,20, -20),to_sfixed (0.33333333,20, -20),to_sfixed (0.25,20, -20),
		to_sfixed (0.2,20, -20),to_sfixed (0.16666666667,20, -20),to_sfixed (0.142857142857,20, -20),to_sfixed (0.125,20, -20));
		variable Sel : integer;
		begin 
			if rst = '1' then
				COUNT <= "1001";
				currentTerm <= to_sfixed (0,20, -20);
				output_internal <= to_sfixed (0,20, -20);
			elsif clk'event and clk = '1' then
				if Start = '1' then
					Done <= '0';
					COUNT <= "0000"; 	
					currentTerm <= to_sfixed (1,20, -20);
					output_internal <= to_sfixed (0,20, -20);
				elsif COUNT /= "1001" then
					Done <= '0';
					output_internal <= resize(output_internal + currentTerm,20, -20);
					currentTerm <= resize(MEM8Xsfixed(to_integer(unsigned(COUNT(2 downto 0)))) * resize(X * currentTerm,20, -20),20, -20);
					COUNT <= COUNT + 1;
				elsif COUNT = "1001" then 
					Done <= '1';
				end if; 
			end if;
	end process;
	Output <= output_internal;
end RTL;
		
